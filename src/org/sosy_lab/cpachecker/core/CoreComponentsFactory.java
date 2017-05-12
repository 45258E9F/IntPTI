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
package org.sosy_lab.cpachecker.core;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.AnalysisWithRefinableEnablerCPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.BDDCPARestrictionAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CustomInstructionRequirementsExtractingAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RestartAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RestartAlgorithmWithARGReplay;
import org.sosy_lab.cpachecker.core.algorithm.RestartWithConditionsAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.BMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.counterexamplecheck.CounterexampleCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.impact.ImpactAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.multientry.BoundedCPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.pcc.AlgorithmWithPropertyCheck;
import org.sosy_lab.cpachecker.core.algorithm.pcc.PartialARGsCombiner;
import org.sosy_lab.cpachecker.core.algorithm.pcc.ProofCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.pcc.ResultCheckAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.HistoryForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.logging.Level;

import javax.annotation.Nullable;

/**
 * Factory class for the three core components of CPAchecker:
 * algorithm, cpa and reached set.
 */
@Options(prefix = "analysis")
public class CoreComponentsFactory {

  public static enum SpecAutomatonCompositionType {
    NONE,
    TARGET_SPEC,
    BACKWARD_TO_ENTRY_SPEC
  }

  @Option(secure = true, description = "use assumption collecting algorithm")
  private boolean collectAssumptions = false;

  @Option(secure = true, name = "algorithm.conditionAdjustment",
      description = "use adjustable conditions algorithm")
  private boolean useAdjustableConditions = false;

  @Option(secure = true, name = "algorithm.CEGAR",
      description = "use CEGAR algorithm for lazy counter-example guided analysis"
          + "\nYou need to specify a refiner with the cegar.refiner option."
          + "\nCurrently all refiner require the use of the ARGCPA.")
  private boolean useCEGAR = false;

  @Option(secure = true, description = "use a second model checking run (e.g., with CBMC or a different CPAchecker configuration) to double-check counter-examples")
  private boolean checkCounterexamples = false;

  @Option(secure = true, name = "checkCounterexamplesWithBDDCPARestriction",
      description = "use counterexample check and the BDDCPA Restriction option")
  private boolean useBDDCPARestriction = false;

  @Option(secure = true, name = "algorithm.BMC",
      description = "use a BMC like algorithm that checks for satisfiability "
          + "after the analysis has finished, works only with PredicateCPA")
  private boolean useBMC = false;

  @Option(secure = true, name = "algorithm.impact",
      description = "Use McMillan's Impact algorithm for lazy interpolation")
  private boolean useImpactAlgorithm = false;

  @Option(secure = true, name = "restartAfterUnknown",
      description = "restart the analysis using a different configuration after unknown result")
  private boolean useRestartingAlgorithm = false;

  @Option(secure = true,
      description = "memorize previously used (incomplete) reached sets after a restart of the analysis")
  private boolean memorizeReachedAfterRestart = false;

  @Option(secure = true, name = "combineARGsAfterRestart",
      description = "combine (partial) ARGs obtained by restarts of the analysis after an unknown result with a different configuration")
  private boolean useARGCombiningAlgorithm = false;

  @Option(secure = true, name = "algorithm.analysisWithEnabler",
      description = "use a analysis which proves if the program satisfies a specified property"
          + " with the help of an enabler CPA to separate different program paths")
  private boolean useAnalysisWithEnablerCPAAlgorithm = false;

  @Option(secure = true, name = "algorithm.proofCheck",
      description = "use a proof check algorithm to validate a previously generated proof")
  private boolean useProofCheckAlgorithm = false;

  @Option(secure = true, name = "algorithm.propertyCheck",
      description = "do analysis and then check "
          + "if reached set fulfills property specified by ConfigurableProgramAnalysisWithPropertyChecker")
  private boolean usePropertyCheckingAlgorithm = false;

  @Option(secure = true, name = "checkProof",
      description = "do analysis and then check analysis result")
  private boolean useResultCheckAlgorithm = false;

  @Option(secure = true, name = "extractRequirements.customInstruction", description = "do analysis and then extract pre- and post conditions for custom instruction from analysis result")
  private boolean useCustomInstructionRequirementExtraction = false;

  @Option(secure = true, name = "restartAlgorithmWithARGReplay",
      description = "run a sequence of analysis, where the previous ARG is inserted into the current ARGReplayCPA.")
  private boolean useRestartAlgorithmWithARGReplay = false;

  @Option(secure = true, name = "algorithm.bounded", description = "use a boundary algorithm to "
      + "restrict the nesting level of call stack and loop stack")
  private boolean useBoundedAlgorithm = false;

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownManager shutdownManager;
  private final ShutdownNotifier shutdownNotifier;

  private final ReachedSetFactory reachedSetFactory;
  private final CPABuilder cpaFactory;

  public CoreComponentsFactory(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    config = pConfig;
    logger = pLogger;
    // new ShutdownManager, it is important for BMCAlgorithm that it gets a ShutdownManager
    // that also affects the CPA it is used with
    shutdownManager = ShutdownManager.createWithParent(pShutdownNotifier);
    shutdownNotifier = shutdownManager.getNotifier();

    config.inject(this);

    reachedSetFactory = new ReachedSetFactory(config);
    cpaFactory = new CPABuilder(config, logger, shutdownNotifier, reachedSetFactory);
  }

  public Algorithm createAlgorithm(
      final ConfigurableProgramAnalysis cpa,
      final String programDenotation, final CFA cfa, @Nullable final MainStatistics stats)
      throws InvalidConfigurationException, CPAException {
    logger.log(Level.FINE, "Creating algorithms");

    Algorithm algorithm;

    if (useProofCheckAlgorithm) {
      logger.log(Level.INFO, "Using Proof Check Algorithm");
      algorithm = new ProofCheckAlgorithm(cpa, config, logger, shutdownNotifier, cfa);
    } else if (useRestartingAlgorithm) {
      logger.log(Level.INFO, "Using Restarting Algorithm");
      algorithm = new RestartAlgorithm(config, logger, shutdownNotifier, programDenotation, cfa);

      if (useARGCombiningAlgorithm) {
        algorithm = new PartialARGsCombiner(algorithm, config, logger, shutdownNotifier);
      }
    } else if (useImpactAlgorithm) {
      algorithm = new ImpactAlgorithm(config, logger, shutdownNotifier, cpa, cfa);

    } else if (useRestartAlgorithmWithARGReplay) {
      algorithm = new RestartAlgorithmWithARGReplay(config, logger, shutdownNotifier, cfa);

    } else {

      if (useBoundedAlgorithm) {
        algorithm = BoundedCPAAlgorithm.create(cpa, logger, config, shutdownNotifier, stats);
      } else {
        algorithm = CPAAlgorithm.create(cpa, logger, config, shutdownNotifier, stats);
      }

      if (useAnalysisWithEnablerCPAAlgorithm) {
        algorithm =
            new AnalysisWithRefinableEnablerCPAAlgorithm(algorithm, cpa, cfa, logger, config,
                shutdownNotifier);
      }

      if (useCEGAR) {
        algorithm = new CEGARAlgorithm(algorithm, cpa, config, logger);
      }

      if (useBMC) {
        algorithm =
            new BMCAlgorithm(algorithm, cpa, config, logger, reachedSetFactory, shutdownManager,
                cfa);
      }

      if (checkCounterexamples) {
        algorithm =
            new CounterexampleCheckAlgorithm(algorithm, cpa, config, logger, shutdownNotifier, cfa,
                programDenotation);
      }

      if (useBDDCPARestriction) {
        algorithm = new BDDCPARestrictionAlgorithm(algorithm, cpa, config, logger);
      }

      if (collectAssumptions) {
        algorithm =
            new AssumptionCollectorAlgorithm(algorithm, cpa, cfa, shutdownNotifier, config, logger);
      }

      if (useAdjustableConditions) {
        algorithm = new RestartWithConditionsAlgorithm(algorithm, cpa, config, logger);
      }

      if (usePropertyCheckingAlgorithm) {
        if (!(cpa instanceof PropertyCheckerCPA)) {
          throw new InvalidConfigurationException(
              "Property checking algorithm requires CPAWithPropertyChecker as Top CPA");
        }
        algorithm =
            new AlgorithmWithPropertyCheck(algorithm, logger, (PropertyCheckerCPA) cpa);
      }

      if (useResultCheckAlgorithm) {
        algorithm = new ResultCheckAlgorithm(algorithm, cpa, cfa, config, logger, shutdownNotifier);
      }

      if (useCustomInstructionRequirementExtraction) {
        algorithm =
            new CustomInstructionRequirementsExtractingAlgorithm(algorithm, cpa, config, logger,
                shutdownNotifier, cfa);
      }
    }

    if (stats != null && algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) algorithm).collectStatistics(stats.getSubStatistics());
    }
    return algorithm;
  }

  public ReachedSetFactory getReachedSetFactory() {
    return reachedSetFactory;
  }

  public ReachedSet createReachedSet() {
    ReachedSet reached = reachedSetFactory.create();

    if (useRestartingAlgorithm || useRestartAlgorithmWithARGReplay) {
      // this algorithm needs an indirection so that it can change
      // the actual reached set instance on the fly
      if (memorizeReachedAfterRestart) {
        reached = new HistoryForwardingReachedSet(reached);
      } else {
        reached = new ForwardingReachedSet(reached);
      }
    }

    return reached;
  }

  public ConfigurableProgramAnalysis createCPA(
      final CFA cfa,
      @Nullable final MainStatistics stats,
      SpecAutomatonCompositionType composeWithSpecificationCPAs)
      throws InvalidConfigurationException, CPAException {
    logger.log(Level.FINE, "Creating CPAs");
    if (stats != null) {
      stats.startCPACreationTimer();
    }
    try {

      if (useRestartingAlgorithm) {
        // hard-coded dummy CPA
        return LocationCPA.factory().set(cfa, CFA.class).setConfiguration(config).createInstance();
      }

      final ConfigurableProgramAnalysis cpa;
      switch (composeWithSpecificationCPAs) {
        case TARGET_SPEC:
          cpa = cpaFactory.buildCPAWithSpecAutomatas(cfa);
          break;
        case BACKWARD_TO_ENTRY_SPEC:
          cpa = cpaFactory.buildCPAWithBackwardSpecAutomatas(cfa);
          break;
        default:
          cpa = cpaFactory.buildCPAs(cfa, null);
      }

      if (stats != null && cpa instanceof StatisticsProvider) {
        ((StatisticsProvider) cpa).collectStatistics(stats.getSubStatistics());
      }
      return cpa;

    } finally {
      if (stats != null) {
        stats.stopCPACreationTimer();
      }
    }
  }
}
