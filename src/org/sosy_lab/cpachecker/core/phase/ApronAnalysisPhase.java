/*
 * Tsmart-BD: The static analysis component of Tsmart platform
 *
 * Copyright (C) 2013-2017  Tsinghua University
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
package org.sosy_lab.cpachecker.core.phase;

import static org.sosy_lab.common.DuplicateOutputStream.mergeStreams;

import com.google.common.io.Closer;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cmdline.ForceTerminationOnShutdown;
import org.sosy_lab.cpachecker.cmdline.ShutdownHook;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.MainCPAStatistics;
import org.sosy_lab.cpachecker.core.MainStatistics;
import org.sosy_lab.cpachecker.core.algorithm.pcc.ProofGenerator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseEmptyResult;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseResult;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseStatus;
import org.sosy_lab.cpachecker.core.phase.util.CPAPhaseExecStrategy;
import org.sosy_lab.cpachecker.core.phase.util.CPAPhases;
import org.sosy_lab.cpachecker.core.phase.util.SequentialExecStrategy;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.apron.ApronState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.globalinfo.BasicIOManager;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * A basic analysis phase should contains two sub-phases: (1) run a single
 * algorithm, (2) collect results.
 * Some features in CPAMain originally should be migrated to this class.
 * A basic analysis phase is an "output" phase, which analyzes a project and generates statistics
 * and other analysis results.
 */
@Options(prefix = "phase.apronAnalysis")
public class ApronAnalysisPhase extends CPAPhase implements StatisticsProvider {

  private final List<CPAPhase> phases;
  private final String outputDirectory;
  private final String rootDirectory;

  @SuppressWarnings("unused")
  public ApronAnalysisPhase(
      String pIdentifier,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownManager pShutdownManager,
      ShutdownNotifier pShutdownNotifier,
      MainStatistics pStatistics)
      throws InvalidConfigurationException {
    // To prevent intervention of multiple phases, we use standalone statistics here
    super(pIdentifier, pConfig, pLogger, pShutdownManager, pShutdownNotifier, new
        MainCPAStatistics(pConfig, pLogger));
    config.inject(this);
    phases = new ArrayList<>(3);

    // set-up the sub-directory and paths for file type converter
    BasicIOManager IOManager = GlobalInfo.getInstance().getIoManager();
    rootDirectory = IOManager.getBasicOutputDirectory();
    outputDirectory = BasicIOManager.concatPath(rootDirectory, id);
    config = BasicIOManager.setupPaths(outputDirectory, config, IOManager.getSecureMode());
  }

  @Override
  protected CPAPhaseStatus prevAction() throws Exception {
    // Create three phases: CFACreatePhase, ApronInvariantRunPhase and AnalyzingResultPhase.
    String algRunID = id.concat(".AlgRun");
    String resultID = id.concat(".Result");
    ConfigurationBuilder configBuilder = Configuration.builder().copyFrom(config);
    Configuration newConfig = configBuilder.build();
    ApronInvariantRunPhase algorithmRunPhase = new ApronInvariantRunPhase(algRunID, newConfig,
        logger, shutdownManager, shutdownNotifier, stats);
    AnalyzingResultPhase resultPhase = new AnalyzingResultPhase(resultID, newConfig, logger,
        shutdownManager, shutdownNotifier, stats);
    algorithmRunPhase.addSuccessor(resultPhase);
    phases.add(algorithmRunPhase);
    phases.add(resultPhase);
    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus postAction() throws Exception {
    currResult = CPAPhaseEmptyResult.createInstance();
    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus runPhase() throws Exception {
    // three phases could only be executed in sequential manner
    ProofGenerator proofGenerator = null;
    ResourceLimitChecker limits;
    BasicOptions options = new BasicOptions();
    try {
      config.inject(options);
      limits = ResourceLimitChecker.fromConfiguration(config, logger, shutdownManager);
      limits.start();
      if (options.doPCC) {
        proofGenerator = new ProofGenerator(config, logger, shutdownNotifier);
      }
    } catch (InvalidConfigurationException e) {
      logger.logUserException(Level.SEVERE, e, "Invalid configuration");
      System.exit(1);
      return CPAPhaseStatus.FAIL;
    }

    ShutdownHook shutdownHook = new ShutdownHook(shutdownManager);
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    ShutdownRequestListener forcedExitOnShutdown = ForceTerminationOnShutdown
        .createShutdownListener(logger, shutdownHook);
    shutdownNotifier.register(forcedExitOnShutdown);

    CPAPhaseExecStrategy execStg = new SequentialExecStrategy();
    CPAPhaseStatus execResult = execStg.exec(phases);

    CPAPhaseResult lastResult = phases.get(phases.size() - 1).getResult();
    if (!(lastResult instanceof CPAcheckerResult)) {
      logger.log(Level.SEVERE, "The last sub-phase of ApronAnalysisPhase must be "
          + "AnalyzingResultPhase.");
      System.exit(1);
      return CPAPhaseStatus.FAIL;
    }
    CPAcheckerResult finalResult = (CPAcheckerResult) lastResult;
    // make up CFA statistics if any
    CFACreatePhase cfaPhase = CPAPhases.extractPhaseByType(prevPhases, CFACreatePhase.class);
    if (cfaPhase != null) {
      List<Statistics> statistics = new ArrayList<>(1);
      cfaPhase.collectStatistics(statistics);
      Statistics onlyStats = statistics.get(0);
      if (onlyStats instanceof MainCPAStatistics) {
        finalResult.updateCFAStatistics((MainCPAStatistics) onlyStats);
      }
    }
    if (proofGenerator != null) {
      proofGenerator.generateProof(finalResult);
    }

    shutdownHook.disable();
    shutdownNotifier.unregister(forcedExitOnShutdown);
    ForceTerminationOnShutdown.cancelPendingTermination();
    limits.cancel();
    Thread.interrupted();

    try {
      printResultAndStatistics(finalResult, options);
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write statistics to file");
    }
    System.out.flush();
    System.err.flush();
    logger.flush();

    for (int i = 0; i < phases.size(); i++) {
      CPAPhase p = phases.get(i);
      if (p instanceof ApronInvariantRunPhase) {
        ApronInvariantRunPhase apronPhase = (ApronInvariantRunPhase) p;
        ReachedSet reached = apronPhase.getReachedSet();
        PartitionedReachedSet pReached = (PartitionedReachedSet) reached;
        LinkedHashMap<AbstractState, Precision> linkedMap = pReached.getReached();
        Iterator<Map.Entry<AbstractState, Precision>> it1 = linkedMap.entrySet().iterator();
        while (it1.hasNext()) {
          Map.Entry<AbstractState, Precision> entry = it1.next();
          ARGState s1 = (ARGState) entry.getKey();
          LocationState locState = null;
          ApronState apronState = null;
          CompositeState s2 = (CompositeState) (s1).getWrappedState();
          List<AbstractState> stateList2 = s2.getWrappedStates();
          for (int j = 0; j < stateList2.size(); j++) {
            if (stateList2.get(j) instanceof LocationState) {
              locState = (LocationState) stateList2.get(j);
            }
            if (stateList2.get(j) instanceof ApronState) {
              apronState = (ApronState) stateList2.get(j);
            }
          }
          if (locState != null && apronState != null) {
            GlobalInfo.getInstance().getApronInvariant().put(locState, apronState);
          }
        }
      }
    }
    Iterator<Map.Entry<LocationState, ApronState>> it =
        GlobalInfo.getInstance().getApronInvariant().entrySet().iterator();
    int num = 0;
    while (it.hasNext()) {
      Map.Entry<LocationState, ApronState> entry = it.next();
      LocationState locState = entry.getKey();
      ApronState apronState = entry.getValue();
      if (locState == null || apronState == null) {
        continue;
      }
      System.out.println(locState.toString() + " --> " + apronState.toString());
      num++;
    }
    System.out
        .println(GlobalInfo.getInstance().getCFAInfo().orNull().getCFA().getAllNodes().size());
    System.out.println(num);
    return execResult;
  }

  private void printResultAndStatistics(CPAcheckerResult pResult, BasicOptions pOptions)
      throws IOException {
    PrintStream console = pOptions.printStatistics ? System.out : null;
    OutputStream file = null;
    Closer closer = Closer.create();
    if (pOptions.exportStatistics && pOptions.exportStatisticsFile != null) {
      try {
        Files.createParentDirs(pOptions.exportStatisticsFile);
        file = closer.register(pOptions.exportStatisticsFile.asByteSink().openStream());
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write statistics to file");
      }
    }
    PrintStream stream = makePrintStream(mergeStreams(console, file));

    try {
      pResult.printStatistics(stream);
      stream.println();
      if (!pOptions.printStatistics) {
        stream = makePrintStream(mergeStreams(System.out, file));
      }
      pResult.printResult(stream);

      if (rootDirectory != null) {
        stream.println("More details about the analysis can be found in the directory \"" +
            rootDirectory + "\".");
      }

      stream.flush();
    } catch (Throwable t) {
      closer.rethrow(t);
    } finally {
      closer.close();
    }
  }

  private PrintStream makePrintStream(OutputStream stream) {
    if (stream instanceof PrintStream) {
      return (PrintStream) stream;
    } else {
      return new PrintStream(stream);
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(stats);
  }

  @Options
  private static class BasicOptions {
    @Option(secure = true, name = "statistics.export", description = "write some statistics to disk")
    private boolean exportStatistics = true;

    @Option(secure = true, name = "statistics.file",
        description = "write some statistics to disk")
    @FileOption(FileOption.Type.OUTPUT_FILE)
    private Path exportStatisticsFile = Paths.get("Statistics.txt");

    @Option(secure = true, name = "statistics.print", description = "print statistics to console")
    private boolean printStatistics = false;

    @Option(secure = true, name = "pcc.proofgen.doPCC", description = "Generate and dump a proof")
    private boolean doPCC = false;
  }

}
