/*
 * IntPTI: integer error fixing by proper-type inference
 * Copyright (c) 2017.
 *
 * Open-source component:
 *
 * CPAchecker
 * Copyright (C) 2007-2014  Dirk Beyer
 *
 * Guava: Google Core Libraries for Java
 * Copyright (C) 2010-2006  Google
 *
 *
 */
package org.sosy_lab.cpachecker.core.algorithm.counterexamplecheck;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Files.DeleteOnCloseFile;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory.SpecAutomatonCompositionType;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

@Options(prefix = "counterexample.checker")
public class CounterexampleCPAChecker implements CounterexampleChecker {

  // The following options will be forced in the counterexample check
  // to have the same value as in the actual analysis.
  private static final ImmutableSet<String> OVERWRITE_OPTIONS = ImmutableSet.of(
      "analysis.machineModel",
      "cpa.predicate.handlePointerAliasing",
      "cpa.predicate.memoryAllocationsAlwaysSucceed"
  );

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final CFA cfa;
  private final String filename;

  private final ARGCPA cpa;

  @Option(secure = true, name = "path.file",
      description = "File name where to put the path specification that is generated "
          + "as input for the counterexample check. A temporary file is used if this is unspecified.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path specFile;

  @Option(secure = true, name = "config",
      description = "configuration file for counterexample checks with CPAchecker")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path configFile = Paths.get("config/valueAnalysis-no-cbmc.properties");

  public CounterexampleCPAChecker(
      Configuration config, LogManager logger,
      ShutdownNotifier pShutdownNotifier, CFA pCfa, String pFilename)
      throws InvalidConfigurationException {
    this.logger = logger;
    this.config = config;
    config.inject(this);
    this.shutdownNotifier = pShutdownNotifier;
    this.cfa = pCfa;
    this.filename = pFilename;
    cpa = null;
  }

  public CounterexampleCPAChecker(
      Configuration config, LogManager logger,
      ShutdownNotifier pShutdownNotifier, CFA pCfa, String pFilename,
      ARGCPA pCpa) throws InvalidConfigurationException {
    this.logger = logger;
    this.config = config;
    config.inject(this);
    this.shutdownNotifier = pShutdownNotifier;
    this.cfa = pCfa;
    this.filename = pFilename;
    cpa = pCpa;
  }


  @Override
  public boolean checkCounterexample(
      ARGState pRootState,
      ARGState pErrorState, Set<ARGState> pErrorPathStates)
      throws CPAException, InterruptedException {

    try {
      if (specFile != null) {
        return checkCounterexample(pRootState, pErrorState, pErrorPathStates, specFile);
      }

      // This temp file will be automatically deleted when the try block terminates.
      try (DeleteOnCloseFile automatonFile = Files
          .createTempFile("counterexample-automaton", ".txt")) {

        return checkCounterexample(pRootState, pErrorState, pErrorPathStates,
            automatonFile.toPath());
      }

    } catch (IOException e) {
      throw new CounterexampleAnalysisFailed(
          "Could not write path automaton to file " + e.getMessage(), e);
    }
  }

  private boolean checkCounterexample(
      ARGState pRootState, ARGState pErrorState, Set<ARGState> pErrorPathStates,
      Path automatonFile) throws IOException, CPAException, InterruptedException {

    try (Writer w = Files.openOutputFile(automatonFile)) {
      ARGUtils.producePathAutomaton(w, pRootState, pErrorPathStates,
          "CounterexampleToCheck", cpa.getCounterexamples().get(pErrorState));
    }

    CFANode entryNode = extractLocation(pRootState);
    LogManager lLogger = logger.withComponentName("CounterexampleCheck");

    try {
      ConfigurationBuilder lConfigBuilder = Configuration.builder()
          .loadFromFile(configFile)
          .setOption("specification", automatonFile.toAbsolutePath().toString());

      for (String option : OVERWRITE_OPTIONS) {
        if (config.hasProperty(option)) {
          lConfigBuilder.copyOptionFrom(config, option);
        } else {
          lConfigBuilder.clearOption(option);
        }
      }

      Configuration lConfig = lConfigBuilder.build();
      ShutdownManager lShutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
      ResourceLimitChecker.fromConfiguration(lConfig, lLogger, lShutdownManager).start();

      CoreComponentsFactory factory =
          new CoreComponentsFactory(lConfig, lLogger, lShutdownManager.getNotifier());
      ConfigurableProgramAnalysis lCpas =
          factory.createCPA(cfa, null, SpecAutomatonCompositionType.TARGET_SPEC);
      Algorithm lAlgorithm = factory.createAlgorithm(lCpas, filename, cfa, null);
      ReachedSet lReached = factory.createReachedSet();
      lReached.add(
          lCpas.getInitialState(entryNode, StateSpacePartition.getDefaultPartition()),
          lCpas.getInitialPrecision(entryNode, StateSpacePartition.getDefaultPartition()));

      lAlgorithm.run(lReached);

      lShutdownManager.requestShutdown("Analysis terminated");
      CPAs.closeCpaIfPossible(lCpas, lLogger);
      CPAs.closeIfPossible(lAlgorithm, lLogger);

      // counterexample is feasible if a target state is reachable
      return from(lReached).anyMatch(IS_TARGET_STATE);

    } catch (InvalidConfigurationException e) {
      throw new CounterexampleAnalysisFailed(
          "Invalid configuration in counterexample-check config: " + e.getMessage(), e);
    } catch (IOException e) {
      throw new CounterexampleAnalysisFailed(e.getMessage(), e);
    } catch (InterruptedException e) {
      shutdownNotifier.shutdownIfNecessary();
      throw new CounterexampleAnalysisFailed("Counterexample check aborted", e);
    }
  }

}
