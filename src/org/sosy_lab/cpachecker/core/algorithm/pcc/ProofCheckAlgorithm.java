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
package org.sosy_lab.cpachecker.core.algorithm.pcc;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PCCStrategy;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.pcc.strategy.PCCStrategyBuilder;
import org.sosy_lab.cpachecker.util.error.DummyErrorState;

import java.io.PrintStream;
import java.util.Collection;
import java.util.logging.Level;

@Options
public class ProofCheckAlgorithm implements Algorithm, StatisticsProvider {

  private static class CPAStatistics implements Statistics {

    private Timer totalTimer = new Timer();
    private Timer readTimer = new Timer();

    @Override
    public String getName() {
      return "Proof Check algorithm";
    }

    @Override
    public void printStatistics(
        PrintStream out, Result pResult,
        ReachedSet pReached) {
      out.println();
      out.println("Proof Checking statistics");
      out.println("-------------------------------------");
      out.println("Total time for proof check algorithm:     " + totalTimer);
      out.println(
          "  Time for reading in proof (not complete time in interleaved modes):  " + readTimer);
    }
  }

  private final CPAStatistics stats = new CPAStatistics();
  private final LogManager logger;


  @Option(secure = true,
      name = "pcc.strategy",
      description = "Qualified name for class which implements proof checking strategy to be used.")
  private String pccStrategy = "org.sosy_lab.cpachecker.pcc.strategy.arg.ARGProofCheckerStrategy";

  private PCCStrategy checkingStrategy;


  public ProofCheckAlgorithm(
      ConfigurableProgramAnalysis cpa, Configuration pConfig,
      LogManager logger, ShutdownNotifier pShutdownNotifier, CFA pCfa)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    checkingStrategy = PCCStrategyBuilder
        .buildStrategy(pccStrategy, pConfig, logger, pShutdownNotifier, cpa, pCfa);

    this.logger = logger;

    logger.log(Level.INFO, "Start reading proof.");
    try {
      stats.totalTimer.start();
      stats.readTimer.start();
      checkingStrategy.readProof();
    } catch (Throwable e) {
      throw new RuntimeException("Failed reading proof.", e);
    } finally {
      stats.readTimer.stop();
      stats.totalTimer.stop();
    }
    logger.log(Level.INFO, "Finished reading proof.");
  }

  protected ProofCheckAlgorithm(
      ConfigurableProgramAnalysis cpa, Configuration pConfig,
      LogManager logger, ShutdownNotifier pShutdownNotifier, ReachedSet pReachedSet, CFA pCfa)
      throws InvalidConfigurationException, InterruptedException {
    pConfig.inject(this);

    checkingStrategy = PCCStrategyBuilder
        .buildStrategy(pccStrategy, pConfig, logger, pShutdownNotifier, cpa, pCfa);
    this.logger = logger;

    if (pReachedSet == null || pReachedSet.hasWaitingState()) {
      throw new IllegalArgumentException(
          "Parameter pReachedSet may not be null and may not have any states in its waitlist.");
    }

    stats.totalTimer.start();
    checkingStrategy.constructInternalProofRepresentation(pReachedSet);
    stats.totalTimer.stop();
  }

  @Override
  public AlgorithmStatus run(final ReachedSet reachedSet)
      throws CPAException, InterruptedException {

    logger.log(Level.INFO, "Proof check algorithm started.");
    stats.totalTimer.start();

    boolean result;
    result = checkingStrategy.checkCertificate(reachedSet);

    stats.totalTimer.stop();
    logger.log(Level.INFO, "Proof check algorithm finished.");

    if (!result) {
      reachedSet
          .add(new DummyErrorState(reachedSet.getFirstState()), SingletonPrecision.getInstance());
    }

    return AlgorithmStatus.SOUND_AND_PRECISE.withSound(result);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    if (checkingStrategy instanceof StatisticsProvider) {
      ((StatisticsProvider) checkingStrategy).collectStatistics(pStatsCollection);
    }
  }
}
