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
package org.sosy_lab.cpachecker.core.phase;

import static org.sosy_lab.common.DuplicateOutputStream.mergeStreams;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.io.Closer;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory.SpecAutomatonCompositionType;
import org.sosy_lab.cpachecker.core.HierarchicalCPAStatistics;
import org.sosy_lab.cpachecker.core.MainStatistics;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.multientry.BoundedAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisMultiInitials;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.phase.entry.StaticEntryFactory;
import org.sosy_lab.cpachecker.core.phase.entry.StaticEntryStrategy;
import org.sosy_lab.cpachecker.core.phase.result.AlgorithmPhaseResult;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseStatus;
import org.sosy_lab.cpachecker.core.phase.util.StatisticsOptions;
import org.sosy_lab.cpachecker.core.reachedset.HierarchicalReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryProvider;
import org.sosy_lab.cpachecker.cpa.arg.ARGPathCounter;
import org.sosy_lab.cpachecker.cpa.boundary.BoundaryCPA;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.globalinfo.BasicIOManager;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * A phase for multi-entry analysis.
 */
@Options(prefix = "phase.me")
public class MultiEntryAlgorithmRunPhase extends CPAPhase {

  private Algorithm algorithm;
  private HierarchicalReachedSet reached;

  private ShutdownManager subShutdownManager;
  private ConfigurableProgramAnalysis subCpa;
  private final String programDenotation;

  @Option(secure = true, name = "stopAfterError", description = "stop after the first error has "
      + "been found")
  private boolean stopAfterError = true;

  @Option(secure = true, name = "analysis", description = "the configuration of main analysis")
  @FileOption(Type.REQUIRED_INPUT_FILE)
  private Path mainAnalyisisConfigFile = Paths.get("config/valueAnalysis-symbolic.properties");

  public MultiEntryAlgorithmRunPhase(
      String pIdentifier,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownManager pShutdownManager,
      ShutdownNotifier pShutdownNotifier,
      MainStatistics pStats)
      throws InvalidConfigurationException {
    super(pIdentifier, pConfig, pLogger, pShutdownManager, pShutdownNotifier, pStats);
    config.inject(this);
    subShutdownManager = null;
    subCpa = null;
    algorithm = null;
    reached = null;
    programDenotation = GlobalInfo.getInstance().getIoManager().getProgramNames();
    assert (pStats instanceof HierarchicalCPAStatistics) : "Hierarchical statistics is required";
  }

  @Override
  protected CPAPhaseStatus prevAction() throws Exception {
    CFAInfo cfaInfo = GlobalInfo.getInstance().getCFAInfo().orNull();
    if (cfaInfo == null) {
      throw new InvalidConfigurationException("Invalid CFA set-up");
    }
    CFA cfa = cfaInfo.getCFA();

    ConfigurationBuilder configBuilder = Configuration.builder();
    configBuilder.copyFrom(config);
    configBuilder.loadFromFile(mainAnalyisisConfigFile);
    Configuration subConfig = configBuilder.build();
    subShutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
    LogManager subLogger = logger.withComponentName("MultiEntryAlgorithmRunPhase");
    CoreComponentsFactory factory = new CoreComponentsFactory(subConfig, subLogger,
        subShutdownManager.getNotifier());
    ReachedSet subReached = factory.createReachedSet();
    // sanity check on reached set
    if (!(subReached instanceof HierarchicalReachedSet)) {
      throw new InvalidConfigurationException("Hierarchical reached set required for multi-entry "
          + "analysis");
    }
    reached = (HierarchicalReachedSet) subReached;
    subCpa = factory.createCPA(cfa, stats, SpecAutomatonCompositionType.TARGET_SPEC);
    if (CPAs.retrieveCPA(subCpa, BoundaryCPA.class) == null) {
      throw new InvalidConfigurationException("Boundary CPA required for multi-entry analysis");
    }
    GlobalInfo.getInstance().setUpInfoFromCPA(subCpa);
    algorithm = factory.createAlgorithm(subCpa, programDenotation, cfa, stats);
    // sanity check on algorithm
    if (!(algorithm instanceof BoundedAlgorithm)) {
      throw new InvalidConfigurationException("Bounded algorithm required for multi-entry "
          + "analysis");
    }
    StaticEntryFactory entryFactory = new StaticEntryFactory(subConfig);
    StaticEntryStrategy strategy = entryFactory.createStrategy();
    // initialize the reached set with the derived entries
    Collection<CFANode> entries = strategy.getInitialEntry(cfa);
    if (entries.isEmpty()) {
      reached.pushEntry(cfa.getMainFunction());
    } else {
      reached.pushEntries(entries);
    }

    SummaryProvider.initialize(subConfig);

    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus postAction() throws Exception {
    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus runPhase() throws Exception {
    AlgorithmStatus runStat = AlgorithmStatus.SOUND_AND_PRECISE;

    BasicIOManager ioManager = GlobalInfo.getInstance().getIoManager();
    String originalDir = BasicIOManager.getCurrentOutputPath(config);
    stats.startAnalysisTimer();
    try {
      while (reached.hasWaitingEntry()) {
        AlgorithmStatus subRunStat = AlgorithmStatus.SOUND_AND_PRECISE;
        Stopwatch watch = Stopwatch.createStarted();
        CFANode entry = reached.peekEntry();
        // we change the output directory for current analysis entry
        String subOutputDir = BasicIOManager.concatPath(originalDir, entry.getFunctionName());
        config = BasicIOManager.setupPaths(subOutputDir, config, ioManager.getSecureMode());

        System.out.print(String.format("> [%d/%d] entry: %s (%s)",
            reached.sizeUsedEntry() + 1,
            reached.sizeWaitEntry() + reached.sizeUsedEntry(),
            entry.getFunctionName(),
            entry.describeFileLocation()
        ));
        System.out.flush();

        boolean popResult = popEntryAndInitReachedSet();
        assert popResult;

        do {
          subRunStat = subRunStat.update(algorithm.run(reached));
        } while (!stopAfterError && reached.hasWaitingState());
        // If the "stop-after-error" flag is set and an error is encountered. In this case, we
        // clear the waitlist first.
        if (reached.hasWaitingState()) {
          reached.dropWaitingStates();
        }

        watch.stop();

        System.out.println(String.format(", used %.3f seconds", watch.elapsed(TimeUnit
            .MILLISECONDS) / 1000.0));

        // UPDATE: here we collect the statistics for this entry once the analysis finishes.
        // Thus, it is unnecessary to keep the history reached set any more, which improves the
        // memory efficiency.
        CPAcheckerResult subResult = summarizeSubResult(subRunStat, reached);
        try {
          printStatistics(subResult);
        } catch (IOException e) {
          logger.logUserException(Level.WARNING, e, "Could not write statistics to file");
        }
        System.out.flush();
        System.err.flush();
        logger.flush();

        runStat = runStat.update(subRunStat);
        // reset the algorithm status for the next entry
        ((BoundedAlgorithm) algorithm).resetStatus();

        // BE AWARE: if you remove this line, your computer will probably be blown up!!!
        reached.clear();

      }
    } finally {
      subShutdownManager.requestShutdown("MultiEntryAlgorithmRunPhase terminated");
      // finally, we should restore the original output directory
      config = BasicIOManager.setupPaths(originalDir, config, ioManager.getSecureMode());
      reached.summarize();
      currResult = new AlgorithmPhaseResult(runStat, null);
    }
    stats.stopAnalysisTimer();
    return CPAPhaseStatus.SUCCESS;
  }

  /**
   * Reinitialize hierarchical reached set by extracting a new CFA node for the next analysis entry.
   *
   * @return whether re-initialization succeeds.
   */
  private boolean popEntryAndInitReachedSet() {
    if (!reached.hasWaitingEntry()) {
      return false;
    }

    CFANode entry = reached.popEntry();
    // reset the path counter for each new entry
    ARGPathCounter.reset();

    Preconditions.checkState(entry != null);

    Collection<AbstractState> initialStates = getInitialStates(
        subCpa,
        entry,
        StateSpacePartition.getDefaultPartition()
    );

    Precision initialPrecision =
        subCpa.getInitialPrecision(entry, StateSpacePartition.getDefaultPartition());
    reached.reInitialize(initialStates, initialPrecision);
    return true;
  }

  /**
   * Compute the initial states for specified CPA. It is possible that one CPA generates several
   * initial states.
   */
  private Collection<AbstractState> getInitialStates(
      ConfigurableProgramAnalysis cpa, CFANode
      node, StateSpacePartition partition) {
    if (cpa instanceof ConfigurableProgramAnalysisMultiInitials) {
      return ((ConfigurableProgramAnalysisMultiInitials) cpa).getInitialStates(node, partition);
    } else {
      return Collections.singleton(cpa.getInitialState(node, partition));
    }
  }

  /**
   * Compute the CPA checker result for the analysis starting from the chosen entry.
   */
  private CPAcheckerResult summarizeSubResult(AlgorithmStatus pStatus, ReachedSet pReached) {
    String violatedPropertyDescription = "";
    Result result;
    Set<Property> violated = AnalyzingResultPhase.findViolatedProperties(pReached);
    if (!violated.isEmpty()) {
      violatedPropertyDescription = Joiner.on(", ").join(violated);
      if (!pStatus.isPrecise()) {
        result = Result.UNKNOWN;
      } else {
        result = Result.FALSE;
      }
    } else {
      if (pReached.hasWaitingState() || !pStatus.isSound()) {
        result = Result.UNKNOWN;
      } else {
        result = Result.TRUE;
      }
    }
    return new CPAcheckerResult(result, violatedPropertyDescription, pReached, stats);
  }

  private void printStatistics(CPAcheckerResult pResult) throws IOException,
                                                                InvalidConfigurationException {
    StatisticsOptions options = new StatisticsOptions(config);
    PrintStream console = options.getPrintStatistics() ? System.out : null;
    OutputStream file = null;
    Closer closer = Closer.create();
    if (options.getExportStatistics() && options.getExportStatisticsFile() != null) {
      try {
        Path exportFile = options.getExportStatisticsFile();
        Files.createParentDirs(exportFile);
        file = closer.register(exportFile.asByteSink().openStream());
      } catch (IOException ex) {
        logger.logUserException(Level.WARNING, ex, "Could not write statistics to file");
      }
    }
    PrintStream stream = BasicAnalysisPhase.makePrintStream(mergeStreams(console, file));
    try {
      ((HierarchicalCPAStatistics) stats).printEntryStatistics(stream, pResult, config);
      stream.println();
      stream.flush();
      options.outputErrorReport();
    } catch (Throwable t) {
      throw closer.rethrow(t);
    } finally {
      closer.close();
    }
  }

}
