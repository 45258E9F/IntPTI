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
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PCCStrategy;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.pcc.strategy.PCCStrategyBuilder;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Options
public class ProofGenerator {

  @Option(secure = true,
      name = "pcc.strategy",
      description = "Qualified name for class which implements certification strategy, hence proof writing, to be used.")
  private String pccStrategy = "org.sosy_lab.cpachecker.pcc.strategy.arg.ARGProofCheckerStrategy";

  @Option(secure = true,
      name = "pcc.sliceProof",
      description = "Make proof more abstract, remove some of the information not needed to prove the property.")
  private boolean slicingEnabled = false;

  private PCCStrategy checkingStrategy;

  private final LogManager logger;
  private final Timer writingTimer = new Timer();

  private final Statistics proofGeneratorStats = new Statistics() {

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
      pOut.println();
      pOut.println(getName() + " statistics");
      pOut.println("------------------------------------");
      pOut.println("Time for proof writing: " + writingTimer);

      if (checkingStrategy != null) {
        for (Statistics stats : checkingStrategy.getAdditionalProofGenerationStatistics()) {
          stats.printStatistics(pOut, pResult, pReached);
        }
      }
    }

    @Override
    public String getName() {
      return "Proof Generation";
    }
  };

  public ProofGenerator(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;

    checkingStrategy = PCCStrategyBuilder
        .buildStrategy(pccStrategy, pConfig, pLogger, pShutdownNotifier, null, null);
  }

  public void generateProof(CPAcheckerResult pResult) {
    // check result
    if (pResult.getResult() != Result.TRUE) {
      logger.log(Level.SEVERE,
          "Proof cannot be generated because checked property not known to be true.");
      return;
    }

    if (pResult.getReached() == null) {
      logger.log(Level.SEVERE, "Proof cannot be generated because reached set not available");
    }

    constructAndWriteProof(pResult.getReached());

    pResult.addProofGeneratorStatistics(proofGeneratorStats);

  }

  private void constructAndWriteProof(UnmodifiableReachedSet pReached) {
    if (slicingEnabled) {
      logger.log(Level.INFO, "Start slicing of proof");
      pReached = new ProofSlicer().sliceProof(pReached);
    }

    // saves the proof
    logger.log(Level.INFO, "Proof Generation started.");

    writingTimer.start();

    checkingStrategy.writeProof(pReached);

    writingTimer.stop();
    logger.log(Level.INFO,
        "Writing proof took " + writingTimer.getMaxTime().formatAs(TimeUnit.SECONDS));

  }

  protected Statistics generateProofUnchecked(final UnmodifiableReachedSet pReached) {
    constructAndWriteProof(pReached);

    return proofGeneratorStats;
  }

}
