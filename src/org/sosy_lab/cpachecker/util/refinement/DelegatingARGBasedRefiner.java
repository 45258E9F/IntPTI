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
package org.sosy_lab.cpachecker.util.refinement;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * This is a {@link ARGBasedRefiner} that delegates each refinement
 * to a list of given {@link ARGBasedRefiner}s (in the given order)
 * until one succeeds.
 */
public final class DelegatingARGBasedRefiner implements ARGBasedRefiner, StatisticsProvider {

  private final List<ARGBasedRefiner> refiners;

  private final List<StatCounter> totalRefinementsSelected;
  private final List<StatCounter> totalRefinementsFinished;
  private final LogManager logger;

  public DelegatingARGBasedRefiner(final LogManager pLogger, final ARGBasedRefiner... pRefiners) {
    logger = pLogger;
    refiners = ImmutableList.copyOf(pRefiners);

    totalRefinementsSelected = new ArrayList<>();
    totalRefinementsFinished = new ArrayList<>();

    for (int i = 0; i < refiners.size(); i++) {
      totalRefinementsSelected.add(new StatCounter("Number of selected refinement"));
      totalRefinementsFinished.add(new StatCounter("Number of finished refinement"));
    }

    assert refiners.size() > 0;
    assert refiners.size() == totalRefinementsSelected.size();
    assert refiners.size() == totalRefinementsFinished.size();
  }

  @Override
  public CounterexampleInfo performRefinementForPath(
      final ARGReachedSet reached,
      ARGPath pErrorPath)
      throws CPAException, InterruptedException {

    CounterexampleInfo cex = null;

    // TODO here, we could sort the refiners to get a better result,
    //      like the score-based sorting from ValueAnalysisDelegatingRefiner

    logger.logf(Level.FINE, "starting refinement with %d refiners", refiners.size());

    for (int i = 0; i < refiners.size(); i++) {
      totalRefinementsSelected.get(i).inc();
      try {

        logger.logf(Level.FINE, "starting refinement %d of %d with %s", i + 1, refiners.size(),
            refiners.get(i).getClass().getSimpleName());
        cex = refiners.get(i).performRefinementForPath(reached, pErrorPath);

        if (cex.isSpurious()) {
          logger.logf(Level.FINE, "refinement %d of %d was successful", i + 1, refiners.size());
          totalRefinementsFinished.get(i).inc();
          break;
        }

      } catch (RefinementFailedException e) {
        if (Reason.RepeatedCounterexample != e.getReason()) {
          throw e; // propagate exception
        } else {
          // ignore and try the next refiner
          logger.logf(Level.FINE, "refinement %d of %d reported repeated counterexample, "
              + "restarting refinement with next refiner", i + 1, refiners.size());
        }
      }
    }

    if (cex == null) {
      // TODO correct reason?
      throw new RefinementFailedException(Reason.RepeatedCounterexample, pErrorPath);
    }

    logger.log(Level.FINE, "refinement finished");

    return Preconditions.checkNotNull(cex);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Statistics() {

      @Override
      public String getName() {
        return DelegatingARGBasedRefiner.class.getSimpleName();
      }

      @Override
      public void printStatistics(
          final PrintStream pOut,
          final Result pResult,
          final ReachedSet pReached) {
        StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);

        for (int i = 0; i < refiners.size(); i++) {
          pOut.println(String
              .format("Analysis %d (%s):", i + 1, refiners.get(i).getClass().getSimpleName()));
          writer.beginLevel().put(totalRefinementsSelected.get(i));
          writer.beginLevel().put(totalRefinementsFinished.get(i));
          writer.spacer();
        }
      }
    });

    for (ARGBasedRefiner refiner : refiners) {
      if (refiner instanceof StatisticsProvider) {
        ((StatisticsProvider) refiner).collectStatistics(pStatsCollection);
      }
    }
  }
}
