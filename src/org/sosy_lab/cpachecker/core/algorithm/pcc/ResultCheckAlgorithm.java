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
package org.sosy_lab.cpachecker.core.algorithm.pcc;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.error.DummyErrorState;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

@Options
public class ResultCheckAlgorithm implements Algorithm, StatisticsProvider {

  private static class ResultCheckStatistics implements Statistics {

    private Timer checkTimer = new Timer();
    private Timer analysisTimer = new Timer();
    private StatisticsProvider checkingStatsProvider = null;
    private Statistics proofGenStats = null;

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
      pOut.println("Time for Verification:          " + analysisTimer);
      pOut.println("Time for Result Check:      " + checkTimer);

      if (checkTimer.getNumberOfIntervals() > 0) {
        pOut.println("Speed up checking:        "
            + ((float) analysisTimer.getSumTime().asNanos()) / checkTimer.getSumTime().asNanos());
      }

      if (proofGenStats != null) {
        proofGenStats.printStatistics(pOut, pResult, pReached);
      }

      if (checkingStatsProvider != null) {
        Collection<Statistics> checkingStats = new ArrayList<>();
        checkingStatsProvider.collectStatistics(checkingStats);
        for (Statistics stats : checkingStats) {
          stats.printStatistics(pOut, pResult, pReached);
        }
      }
    }

    @Override
    public String getName() {
      return "ResultCheckAlgorithm";
    }

  }

  private final LogManager logger;
  private final Configuration config;
  private final ShutdownNotifier shutdownNotifier;
  private final Algorithm analysisAlgorithm;
  private final ConfigurableProgramAnalysis cpa;
  private final CFA analyzedProgram;
  private final ResultCheckStatistics stats;
  @Option(secure = true,
      name = "pcc.resultcheck.writeProof",
      description = "Enable to write proof and read it again for validation instead of using the in memory solution")
  private boolean writeProof = false;
  @Option(secure = true,
      name = "pcc.resultcheck.checkerConfig",
      description = "Configuration for proof checking if differs from analysis configuration")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path checkerConfig;

  public ResultCheckAlgorithm(
      Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa, CFA pCfa,
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    analysisAlgorithm = pAlgorithm;
    analyzedProgram = pCfa;
    cpa = pCpa;
    logger = pLogger;
    config = pConfig;
    shutdownNotifier = pShutdownNotifier;
    stats = new ResultCheckStatistics();
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus status;

    logger.log(Level.INFO, "Start analysis.");

    try {
      stats.analysisTimer.start();
      status = analysisAlgorithm.run(pReachedSet);
    } finally {
      stats.analysisTimer.stop();
      logger.log(Level.INFO, "Analysis stopped.");
    }

    if (status.isSound() && !pReachedSet.hasWaitingState()) {
      logger.log(Level.INFO, "Analysis successful.", "Start checking analysis result");
      try {
        if (writeProof) {
          status = writeProofAndValidateWrittenProof(pReachedSet);
        } else {
          status = resultCheckingWithoutWritingProof(pReachedSet);
        }
      } catch (InvalidConfigurationException e) {
        status = status.withSound(false);
      } catch (InterruptedException e1) {
        logger.log(Level.INFO, "Timed out. Checking incomplete.");
        return status.withSound(false);
      } finally {
        if (stats.checkTimer.isRunning()) {
          stats.checkTimer.stop();
        }
        logger.log(Level.INFO, "Stop checking analysis result.");
      }

      if (status.isSound()) {
        logger.log(Level.INFO, "Analysis result checked successfully.");
        return status;
      } else {
        pReachedSet.add(new DummyErrorState(pReachedSet.getFirstState()),
            SingletonPrecision.getInstance());
      }
      logger.log(Level.INFO, "Analysis result could not be checked.");

    } else {
      logger.log(Level.WARNING, "Analysis incomplete.");
    }

    return status;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (analysisAlgorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) analysisAlgorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
  }

  private AlgorithmStatus resultCheckingWithoutWritingProof(final ReachedSet pVerificationResult)
      throws InvalidConfigurationException, InterruptedException, CPAException {
    stats.checkTimer.start();
    ProofCheckAlgorithm checker = new ProofCheckAlgorithm(cpa, config, logger, shutdownNotifier,
        pVerificationResult, analyzedProgram);
    stats.checkingStatsProvider = checker;
    return checker.run(initializeReachedSetForChecking(config, cpa));
  }

  private ReachedSet initializeReachedSetForChecking(
      Configuration pConfig,
      ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException {
    CoreComponentsFactory factory = new CoreComponentsFactory(pConfig, logger, shutdownNotifier);
    ReachedSet reached = factory.createReachedSet();

    reached.add(pCpa.getInitialState(analyzedProgram.getMainFunction(),
        StateSpacePartition.getDefaultPartition()),
        pCpa.getInitialPrecision(analyzedProgram.getMainFunction(),
            StateSpacePartition.getDefaultPartition()));

    return reached;
  }

  private AlgorithmStatus writeProofAndValidateWrittenProof(final ReachedSet pVerificationResult)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    logger.log(Level.INFO, "Write Proof");
    ProofGenerator proofGen = new ProofGenerator(config, logger, shutdownNotifier);
    stats.proofGenStats = proofGen.generateProofUnchecked(pVerificationResult);

    Configuration checkConfig = config;
    ConfigurableProgramAnalysis checkerCPA = cpa;
    if (checkerConfig != null) {
      try {
        checkConfig = Configuration.builder().copyFrom(config).loadFromFile(checkerConfig).build();
        CoreComponentsFactory factory =
            new CoreComponentsFactory(checkConfig, logger, shutdownNotifier);
        checkerCPA =
            new CPABuilder(checkConfig, logger, shutdownNotifier, factory.getReachedSetFactory())
                .buildCPAWithSpecAutomatas(analyzedProgram);

      } catch (IOException e) {
        logger.log(Level.SEVERE, "Cannot read proof checking configuration.");
        return AlgorithmStatus.SOUND_AND_PRECISE.withSound(false);
      }
    }

    stats.checkTimer.start();
    ProofCheckAlgorithm checker =
        new ProofCheckAlgorithm(checkerCPA, checkConfig, logger, shutdownNotifier, analyzedProgram);
    stats.checkingStatsProvider = checker;
    return checker.run(initializeReachedSetForChecking(checkConfig, checkerCPA));
  }
}
