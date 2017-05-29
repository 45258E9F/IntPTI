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
import org.sosy_lab.cpachecker.core.bugfix.FixProvider;
import org.sosy_lab.cpachecker.core.bugfix.FixProvider.BugCategory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisMultiInitials;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.phase.entry.StaticEntryFactory;
import org.sosy_lab.cpachecker.core.phase.entry.StaticEntryStrategy;
import org.sosy_lab.cpachecker.core.phase.fix.IntegerFixGenerationPhase;
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
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Program repair phase, its workflow is presented as follows:
 *
 * Run algorithm -> Fix generation -> Fix application
 * |                 |
 * +----<---<---<----+
 *
 * This phase should be put in the basic analysis phase after CFA creation.
 * Iterative analysis algorithm is similar with multi-entry algorithm, which is also a more
 * generic form of simple CPA algorithm.
 */
@Options(prefix = "phase.repair")
public class ProgramRepairPhase extends CPAPhase {

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
  private Path mainAnalysisConfig = Paths.get("config/valueAnalysis-symbolic.properties");

  @Option(secure = true, name = "category", description = "specific category of program "
      + "repair. For now only one category is supported on each run")
  private BugCategory repairCategory = null;

  @Option(secure = true, name = "interactive", description = "whether the fix application is "
      + "user-interactive")
  private boolean isInteractive = false;

  public ProgramRepairPhase(
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
    assert (pStats instanceof HierarchicalCPAStatistics) : "Hierarchical statistics required for "
        + "program repair";
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
    configBuilder.loadFromFile(mainAnalysisConfig);
    Configuration subConfig = configBuilder.build();
    subShutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
    LogManager subLogger = logger.withComponentName("ProgramRepairPhase");
    CoreComponentsFactory factory = new CoreComponentsFactory(subConfig, subLogger,
        subShutdownManager.getNotifier());
    ReachedSet subReached = factory.createReachedSet();
    if (!(subReached instanceof HierarchicalReachedSet)) {
      throw new InvalidConfigurationException("Hierarchical reached set required for program "
          + "repair");
    }
    reached = (HierarchicalReachedSet) subReached;
    subCpa = factory.createCPA(cfa, stats, SpecAutomatonCompositionType.TARGET_SPEC);
    if (CPAs.retrieveCPA(subCpa, BoundaryCPA.class) == null) {
      throw new InvalidConfigurationException("Boundary CPA required for multi-entry analysis");
    }
    GlobalInfo.getInstance().setUpInfoFromCPA(subCpa);
    algorithm = factory.createAlgorithm(subCpa, programDenotation, cfa, stats);
    if (!(algorithm instanceof BoundedAlgorithm)) {
      throw new InvalidConfigurationException("Bounded algorithm required for program repair");
    }
    // entry selection strategy
    StaticEntryFactory entryFactory = new StaticEntryFactory(subConfig);
    StaticEntryStrategy strategy = entryFactory.createStrategy();
    Collection<CFANode> entries = strategy.getInitialEntry(cfa);
    if (entries.isEmpty()) {
      reached.pushEntry(cfa.getMainFunction());
    } else {
      reached.pushEntries(entries);
    }

    // summary computation is also required for multi-entry analysis
    SummaryProvider.initialize(subConfig);

    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus runPhase() throws Exception {
    AlgorithmStatus runStat = AlgorithmStatus.SOUND_AND_PRECISE;

    BasicIOManager ioManager = GlobalInfo.getInstance().getIoManager();
    String originalDir = BasicIOManager.getCurrentOutputPath(config);

    CPAPhase fixGenPhase = null;
    CPAPhase fixAppPhase = null;

    Stopwatch analysisWatch = Stopwatch.createUnstarted();
    Stopwatch generationWatch = Stopwatch.createUnstarted();
    Stopwatch appWatch = Stopwatch.createUnstarted();

    Stopwatch passWatch;

    stats.startAnalysisTimer();
    try {
      while (reached.hasWaitingEntry()) {

        // STEP 1: analyze from a specified entry node

        AlgorithmStatus subRunStat = AlgorithmStatus.SOUND_AND_PRECISE;
        CFANode entry = reached.peekEntry();
        // we change the output directory for current analysis entry
        String subOutputDir = BasicIOManager.concatPath(originalDir, entry.getFunctionName());
        config = BasicIOManager.setupPaths(subOutputDir, config, ioManager.getSecureMode());

        System.out.print(String.format("> [%d/%d] entry: %s (%s)", reached
                .sizeUsedEntry() + 1, reached.sizeWaitEntry() + reached.sizeUsedEntry(),
            entry.getFunctionName(), entry.describeFileLocation()));
        System.out.flush();

        passWatch = Stopwatch.createStarted();
        analysisWatch.start();

        boolean popResult = popEntryAndInitReachedSet();
        assert popResult;

        do {
          subRunStat = subRunStat.update(algorithm.run(reached));
        } while (!stopAfterError && reached.hasWaitingState());
        if (reached.hasWaitingState()) {
          reached.dropWaitingStates();
        }

        analysisWatch.stop();
        passWatch.stop();
        System.out.println(String.format(" used %.3f s", passWatch.elapsed(TimeUnit
            .MILLISECONDS) / 1000.0));

        CPAcheckerResult subResult = summarizeSubResult(subRunStat, reached);
        try {
          printStatistics(subResult);
        } catch (IOException e) {
          logger.logUserException(Level.WARNING, e, "Could not write statistics to file");
        }
        logger.flush();

        runStat = runStat.update(subRunStat);
        ((BoundedAlgorithm) algorithm).resetStatus();
        reached.clear();

        // STEP 2: generate fixes using the collected information, and clear them for the next
        // analysis entry

        if (fixGenPhase == null) {
          Class<? extends CPAPhase> genPhaseClass = FixProvider.getFixGenClass(repairCategory);
          if (genPhaseClass != null) {
            // create a new fix generation class
            try {
              Class<?>[] paramTypes = {String.class, Configuration.class, LogManager.class,
                  ShutdownManager.class, ShutdownNotifier.class, MainStatistics.class};
              Constructor<?> constructor = genPhaseClass.getConstructor(paramTypes);
              // copy to a new configuration to prevent intervention
              ConfigurationBuilder configBuilder = Configuration.builder().copyFrom(config);
              Configuration newConfig = configBuilder.build();
              Object[] args = {id.concat(".FixGen"), newConfig, logger, shutdownManager,
                  shutdownNotifier, stats};
              Object phaseInstance = constructor.newInstance(args);
              assert (phaseInstance instanceof CPAPhase);
              fixGenPhase = (CPAPhase) phaseInstance;
            } catch (NoSuchMethodException e) {
              throw new IllegalArgumentException("Missing required constructor");
            } catch (Exception e) {
              e.printStackTrace();
              throw new IllegalArgumentException("Failed to instantiate a fix generation phase " +
                  genPhaseClass.getName());
            }
          }
        }
        // If no fix generation phase is found, the program repair phase is equivalent with the
        // multi-entry analysis phase.
        if (fixGenPhase != null) {
          generationWatch.start();

          CPAPhaseStatus subStatus = fixGenPhase.run();

          generationWatch.stop();

          if (subStatus != CPAPhaseStatus.SUCCESS) {
            return subStatus;
          }
        }

        // intermediate information for fix generation should be reset after handling each entry
        FixProvider.clearStatus(repairCategory);
      }
    } finally {
      subShutdownManager.requestShutdown("ProgramRepairPhase terminated");
      config = BasicIOManager.setupPaths(originalDir, config, ioManager.getSecureMode());
      reached.summarize();
      currResult = new AlgorithmPhaseResult(runStat, null);
    }
    stats.stopAnalysisTimer();

    // STEP 3: apply fixes to the original source file
    // Though it is possible that the phase fails in some entries, we can still apply the
    // generated fixes (which are possibly incomplete).
    if (fixAppPhase == null) {
      Class<? extends CPAPhase> appPhaseClass;
      if (isInteractive) {
        appPhaseClass = FixProvider.getInteractiveAppPhase(repairCategory);
      } else {
        appPhaseClass = FixProvider.getFixAppClass(repairCategory);
      }
      if (appPhaseClass != null) {
        try {
          Class<?>[] paramTypes = {String.class, Configuration.class, LogManager.class,
              ShutdownManager.class, ShutdownNotifier.class, MainStatistics.class};
          Constructor<?> constructor = appPhaseClass.getConstructor(paramTypes);
          ConfigurationBuilder configBuilder = Configuration.builder().copyFrom(config);
          Configuration newConfig = configBuilder.build();
          Object[] args = {id.concat(".FixApp"), newConfig, logger, shutdownManager,
              shutdownNotifier, stats};
          Object phaseInstance = constructor.newInstance(args);
          assert (phaseInstance instanceof CPAPhase);
          fixAppPhase = (CPAPhase) phaseInstance;
        } catch (NoSuchMethodException e) {
          throw new IllegalArgumentException("Missing required constructor");
        } catch (Exception e) {
          e.printStackTrace();
          throw new IllegalArgumentException("Failed to instantiate a fix application phase " +
              appPhaseClass.getName());
        }
      }
    }
    if (fixAppPhase != null) {
      appWatch.start();

      CPAPhaseStatus subStatus = fixAppPhase.run();

      appWatch.stop();

      if (subStatus != CPAPhaseStatus.SUCCESS) {
        return subStatus;
      }
    }

    // output total punishment
    if (fixGenPhase != null) {
      System.out.println(String.format("Punishment: %d", ((IntegerFixGenerationPhase)
          fixGenPhase).getTotalPunishment()));
    }
    // output timer info
    System.out.println(String.format("ANALYSIS: %.3f", analysisWatch.elapsed(TimeUnit
        .MILLISECONDS) / 1000.0));
    System.out.println(String.format("FIXGEN: %.3f", generationWatch.elapsed(TimeUnit
        .MILLISECONDS) / 1000.0));
    System.out.println(String.format("FIXAPP: %.3f", appWatch.elapsed(TimeUnit.MILLISECONDS) /
        1000.0));

    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus postAction() throws Exception {
    return CPAPhaseStatus.SUCCESS;
  }

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

  private Collection<AbstractState> getInitialStates(
      ConfigurableProgramAnalysis cpa, CFANode
      node, StateSpacePartition partition) {
    if (cpa instanceof ConfigurableProgramAnalysisMultiInitials) {
      return ((ConfigurableProgramAnalysisMultiInitials) cpa).getInitialStates(node, partition);
    } else {
      return Collections.singleton(cpa.getInitialState(node, partition));
    }
  }

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
