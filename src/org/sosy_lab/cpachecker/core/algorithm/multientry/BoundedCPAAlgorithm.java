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
package org.sosy_lab.cpachecker.core.algorithm.multientry;

import static org.sosy_lab.cpachecker.cpa.boundary.info.LoopBoundedInfo.LoopOutOfBoundReason.MAX_ITERATION_REACHED;

import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AlgorithmIterationListener;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCovering;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.summary.SummaryAcceptableState;
import org.sosy_lab.cpachecker.core.phase.entry.DynamicEntryFactory;
import org.sosy_lab.cpachecker.core.phase.entry.DynamicEntryStrategy;
import org.sosy_lab.cpachecker.core.reachedset.HierarchicalReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.summary.manage.FunctionSummaryStore;
import org.sosy_lab.cpachecker.core.summary.manage.LoopSummaryStore;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryProvider;
import org.sosy_lab.cpachecker.cpa.arg.ARGMergeJoinCPAEnabledAnalysis;
import org.sosy_lab.cpachecker.cpa.boundary.BoundaryState;
import org.sosy_lab.cpachecker.cpa.boundary.BoundaryState.BoundaryFlag;
import org.sosy_lab.cpachecker.cpa.boundary.info.BoundedInfo;
import org.sosy_lab.cpachecker.cpa.boundary.info.EmptyBoundedInfo;
import org.sosy_lab.cpachecker.cpa.boundary.info.FunctionBoundedInfo;
import org.sosy_lab.cpachecker.cpa.boundary.info.LoopBoundedInfo;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.annotation.Nullable;

public class BoundedCPAAlgorithm implements BoundedAlgorithm, StatisticsProvider {

  /* ********** */
  /* statistics */
  /* ********** */

  private static class BoundedCPAStatistics implements Statistics {

    private Timer totalTimer = new Timer();
    private Timer transferTimer = new Timer();
    private Timer precisionTimer = new Timer();
    private Timer mergeTimer = new Timer();
    private Timer stopTimer = new Timer();
    private Timer forcedCoveringTimer = new Timer();

    private int countIterations = 0;
    private int maxWaitlistSize = 0;
    private long countWaitlistSize = 0;
    private int countSuccessors = 0;
    private int maxSuccessors = 0;
    private int countMerge = 0;
    private int countStop = 0;
    private int countBreak = 0;

    @Override
    public void printStatistics(
        PrintStream out, Result result, ReachedSet reached) {
      out.println("Number of iterations:            " + countIterations);
      if (countIterations == 0) {
        // Statistics not relevant, prevent division by zero
        return;
      }
      out.println("Max size of waitlist:            " + maxWaitlistSize);
      out.println("Average size of waitlist:        " + countWaitlistSize
          / countIterations);
      out.println("Number of computed successors:   " + countSuccessors);
      out.println("Max successors for one state:    " + maxSuccessors);
      out.println("Number of times merged:          " + countMerge);
      out.println("Number of times stopped:         " + countStop);
      out.println("Number of times breaked:         " + countBreak);
      out.println();
      out.println("Total time for CPA algorithm:     " + totalTimer + " (Max: " +
          totalTimer.getMaxTime().formatAs(TimeUnit.SECONDS) + ")");
      if (forcedCoveringTimer.getNumberOfIntervals() > 0) {
        out.println("  Time for forced covering:       " + forcedCoveringTimer);
      }
      out.println("  Time for precision adjustment:  " + precisionTimer);
      out.println("  Time for transfer relation:     " + transferTimer);
      if (mergeTimer.getNumberOfIntervals() > 0) {
        out.println("  Time for merge operator:        " + mergeTimer);
      }
      out.println("  Time for stop operator:         " + stopTimer);
    }

    @Override
    public String getName() {
      return "Bounded CPA algorithm";
    }

    void reset() {
      totalTimer = new Timer();
      transferTimer = new Timer();
      precisionTimer = new Timer();
      mergeTimer = new Timer();
      stopTimer = new Timer();
      forcedCoveringTimer = new Timer();

      countIterations = 0;
      maxWaitlistSize = 0;
      countWaitlistSize = 0;
      countSuccessors = 0;
      maxSuccessors = 0;
      countMerge = 0;
      countStop = 0;
      countBreak = 0;
    }

  }

  /* ******* */
  /* factory */
  /* ******* */

  @Options(prefix = "bound.cpa")
  public static class BoundedCPAAlgorithmFactory {

    @Option(secure = true, name = "forcedCovering", description = "Strategy for forced covering")
    private Class<? extends ForcedCovering> forcedCoveringClass = null;

    @Option(secure = true, description = "Report UNKNOWN instead of FALSE")
    private boolean reportFalseAsUnknown = false;

    private final ForcedCovering forcedCovering;

    private final ConfigurableProgramAnalysis cpa;
    private final LogManager logger;
    private final ShutdownNotifier shutdownNotifier;
    private final AlgorithmIterationListener iterationListener;

    private final DynamicEntryStrategy entryStrategy;

    BoundedCPAAlgorithmFactory(
        ConfigurableProgramAnalysis pCpa, LogManager pLogger,
        Configuration pConfig, ShutdownNotifier pShutdownNotifier,
        @Nullable AlgorithmIterationListener pIterationListener)
        throws InvalidConfigurationException {
      pConfig.inject(this);
      cpa = pCpa;
      logger = pLogger;
      shutdownNotifier = pShutdownNotifier;
      iterationListener = pIterationListener;
      if (forcedCoveringClass != null) {
        forcedCovering = Classes.createInstance(ForcedCovering.class, forcedCoveringClass,
            new Class<?>[]{Configuration.class, LogManager.class, ConfigurableProgramAnalysis
                .class},
            new Object[]{pConfig, pLogger, pCpa});
      } else {
        forcedCovering = null;
      }
      DynamicEntryFactory factory = new DynamicEntryFactory(pConfig);
      entryStrategy = factory.createStrategy();
    }

    public BoundedCPAAlgorithm newInstance() {
      return new BoundedCPAAlgorithm(cpa, logger, shutdownNotifier, forcedCovering, entryStrategy,
          iterationListener, reportFalseAsUnknown);
    }

  }

  public static BoundedCPAAlgorithm create(
      ConfigurableProgramAnalysis cpa, LogManager logger,
      Configuration config,
      ShutdownNotifier pShutdownNotifier,
      @Nullable AlgorithmIterationListener pIterationListener)
      throws InvalidConfigurationException {
    return new BoundedCPAAlgorithmFactory(cpa, logger, config, pShutdownNotifier,
        pIterationListener).newInstance();
  }

  /* *********** */
  /* bounded CPA */
  /* *********** */

  private final BoundedCPAStatistics stats = new BoundedCPAStatistics();

  private final ForcedCovering forcedCovering;

  private final TransferRelation transferRelation;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final PrecisionAdjustment precisionAdjustment;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final AlgorithmIterationListener iterationListener;
  private final AlgorithmStatus status;

  private final DynamicEntryStrategy entryStrategy;

  private BoundedCPAAlgorithm(
      ConfigurableProgramAnalysis pCpa,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      ForcedCovering pForcedCovering,
      DynamicEntryStrategy pStrategy,
      AlgorithmIterationListener pIterationListener,
      boolean pIsImprecise) {
    transferRelation = pCpa.getTransferRelation();
    mergeOperator = pCpa.getMergeOperator();
    stopOperator = pCpa.getStopOperator();
    precisionAdjustment = pCpa.getPrecisionAdjustment();
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    forcedCovering = pForcedCovering;
    entryStrategy = pStrategy;
    iterationListener = pIterationListener;
    status = AlgorithmStatus.SOUND_AND_PRECISE.withPrecise(pIsImprecise);
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet)
      throws CPAException, InterruptedException {
    HierarchicalReachedSet reached;
    if (!(reachedSet instanceof HierarchicalReachedSet)) {
      throw new IllegalArgumentException("Hierarchical reached set required for bounded CPA "
          + "algorithm");
    }
    reached = (HierarchicalReachedSet) reachedSet;
    stats.totalTimer.start();
    try {
      return run0(reached);
    } finally {
      stats.totalTimer.stopIfRunning();
      stats.transferTimer.stopIfRunning();
      stats.precisionTimer.stopIfRunning();
      stats.mergeTimer.stopIfRunning();
      stats.stopTimer.stopIfRunning();
      stats.forcedCoveringTimer.stopIfRunning();
    }
  }

  private AlgorithmStatus run0(final HierarchicalReachedSet reached) throws CPAException,
                                                                            InterruptedException {
    while (reached.hasWaitingState()) {
      shutdownNotifier.shutdownIfNecessary();
      stats.countIterations++;
      int size = reached.getWaitlist().size();
      if (size >= stats.maxWaitlistSize) {
        stats.maxWaitlistSize = size;
      }
      stats.countWaitlistSize += size;

      final AbstractState state = reached.popFromWaitlist();
      final Precision precision = reached.getPrecision(state);

      try {
        if (handleState(state, precision, reached)) {
          return status;
        }
      } catch (Exception e) {
        reached.reAddToWaitlist(state);
        throw e;
      }

      if (iterationListener != null) {
        iterationListener.afterAlgorithmIteration(this, reached);
      }
    }
    return status;
  }

  private boolean handleState(
      final AbstractState state, final Precision precision,
      final HierarchicalReachedSet reached)
      throws CPAException, InterruptedException {
    if (forcedCovering != null) {
      stats.forcedCoveringTimer.start();
      try {
        boolean stop = forcedCovering.tryForcedCovering(state, precision, reached);
        if (stop) {
          return false;
        }
      } finally {
        stats.forcedCoveringTimer.stop();
      }
    }

    stats.transferTimer.start();
    Collection<? extends AbstractState> successors;
    try {
      successors = transferRelation.getAbstractSuccessors(state,
          Lists.<AbstractState>newArrayList(), precision);
    } finally {
      stats.transferTimer.stop();
    }
    // Check each successor state to find ones with exceeded call stack / loop stack boundary. If
    // such state exists, we remove them and apply function / loop summary to its predecessor
    // state and then we could have some new states to replace such state. Then, we perform
    // precision adjustment on the collection of new states.
    Set<AbstractState> newSuccessors = new HashSet<>();
    CFANode oldLocation = AbstractStates.extractLocation(state);
    for (AbstractState successor : successors) {
      // STEP 1: try to derive a new entry
      CFANode newEntry = entryStrategy.getEntry(successor);
      if (newEntry != null) {
        reached.pushEntry(newEntry);
      }
      // STEP 2: examine boundary state
      BoundaryState boundaryState = AbstractStates.extractStateByType(successor, BoundaryState
          .class);
      assert (boundaryState != null);
      boolean underAbstract = boundaryState.underAbstractExecution();
      BoundedInfo<?> boundedInfo = boundaryState.getBoundedInfo(oldLocation, AbstractStates
          .extractLocation(successor));
      if (boundedInfo instanceof EmptyBoundedInfo) {
        // If the returned bounded information is empty, we should further check if current state
        // has LOOP or FUNCTION flag set. We add the successor state only if no such flag is set.
        // Otherwise, out-of-bound state is directly discarded.
        if (boundaryState.getFlag() == BoundaryFlag.NONE) {
          newSuccessors.add(successor);
        }
      } else if (boundedInfo instanceof FunctionBoundedInfo) {
        List<FunctionSummaryStore<?>> summaryStores = SummaryProvider.getFunctionSummary();
        String functionName = ((FunctionBoundedInfo) boundedInfo).getBoundedObject();
        List<SummaryInstance> summaryList = new ArrayList<>();
        for (FunctionSummaryStore<?> summaryStore : summaryStores) {
          SummaryInstance instance = summaryStore.query(functionName);
          if (instance != null) {
            summaryList.add(instance);
          }
        }
        CFAEdge inEdge = boundedInfo.getEntry();
        CFAEdge outEdge = Iterables.getOnlyElement(boundedInfo.getExit());
        if (state instanceof SummaryAcceptableState) {
          Collection<? extends AbstractState> statesAfterSummary = ((SummaryAcceptableState)
              state).applyFunctionSummary(summaryList, inEdge, outEdge,
              Lists.<AbstractState>newArrayList());
          newSuccessors.addAll(statesAfterSummary);
        } else {
          // Otherwise, summary has no effect on the analysis. Thus, the analysis goes on as
          // there is no summary or boundary information.
          newSuccessors.add(successor);
        }
      } else if (boundedInfo instanceof LoopBoundedInfo) {
        // STEP 1: some preparations
        Loop loop = ((LoopBoundedInfo) boundedInfo).getBoundedObject();
        List<SummaryInstance> externalSummaryList = new ArrayList<>();
        for (LoopSummaryStore<?> summaryStore : SummaryProvider.getExternalLoopSummary()) {
          SummaryInstance instance = summaryStore.query(loop);
          if (instance != null) {
            externalSummaryList.add(instance);
          }
        }
        CFAEdge inEdge = boundedInfo.getEntry();
        Collection<CFAEdge> outEdges = boundedInfo.getExit();
        // STEP 2: apply loop summary to skip or execute the loop on the abstract manner
        if (successor instanceof SummaryAcceptableState) {
          // CASE I: current successor is under abstract execution mode AND the loop bound reason
          // is MAX_ITERATION_REACHED, which occurs when the abstract execution encounters a new
          // loop.
          boolean shouldInnerSummary = underAbstract && ((LoopBoundedInfo) boundedInfo).getReason
              () == MAX_ITERATION_REACHED;
          // CASE II: current successor is not under abstract execution mode but the maximum
          // allowed iteration is reached. We apply internal loop summary only at the outermost
          // loop (aka. the loop of depth 1)
          shouldInnerSummary = shouldInnerSummary || (!underAbstract && boundaryState.getLoopDepth
              () == 1);
          if (shouldInnerSummary) {
            List<SummaryInstance> internalSummaryList = new ArrayList<>();
            for (LoopSummaryStore<?> summaryStore : SummaryProvider.getInternalLoopSummary()) {
              SummaryInstance instance = summaryStore.query(loop);
              if (instance != null) {
                internalSummaryList.add(instance);
              }
            }
            // If there is no summary instance, no abstract states are derived
            if (!internalSummaryList.isEmpty()) {
              Collection<? extends AbstractState> abstractStates = ((SummaryAcceptableState)
                  successor).applyInternalLoopSummary(internalSummaryList, inEdge, Lists
                  .<AbstractState>newArrayList());
              newSuccessors.addAll(abstractStates);
            }
          }

          // We should always apply external summary to skip this loop
          Multimap<CFAEdge, AbstractState> statesAfterSummary = ((SummaryAcceptableState)
              successor).applyExternalLoopSummary(externalSummaryList, inEdge, outEdges, Lists
              .<AbstractState>newArrayList());
          newSuccessors.addAll(statesAfterSummary.values());
        } else {
          newSuccessors.add(successor);
        }
      } else {
        logger.log(Level.SEVERE, "Unrecognized bounded info, BoundedCPAAlgorithm just ignores it");
        newSuccessors.add(successor);
      }
    }
    int numSuccessors = newSuccessors.size();
    stats.countSuccessors += numSuccessors;
    stats.maxSuccessors = Math.max(numSuccessors, stats.maxSuccessors);

    for (AbstractState successor : Iterables.consumingIterable(newSuccessors)) {
      stats.precisionTimer.start();
      PrecisionAdjustmentResult precisionAdjustmentResult;
      try {
        Optional<PrecisionAdjustmentResult> precisionAdjustmentOptional = precisionAdjustment
            .prec(successor, precision, reached, Functions.<AbstractState>identity(), successor);
        if (!precisionAdjustmentOptional.isPresent()) {
          continue;
        }
        precisionAdjustmentResult = precisionAdjustmentOptional.get();
      } finally {
        stats.precisionTimer.stop();
      }
      successor = precisionAdjustmentResult.abstractState();
      Precision successorPrecision = precisionAdjustmentResult.precision();
      Action action = precisionAdjustmentResult.action();
      if (action == Action.BREAK) {
        stats.stopTimer.start();
        boolean stop;
        try {
          stop = stopOperator.stop(successor, reached.getReached(successor), successorPrecision);
        } finally {
          stats.stopTimer.stop();
        }
        if (AbstractStates.isTargetState(successor) && stop) {
          stats.countStop++;
          logger.log(Level.FINER, "Break was signalled but ignored because the state is covered.");
          continue;
        } else {
          stats.countBreak++;
          logger.log(Level.FINER, "Break signalled, BoundedCPAAlgorithm will stop.");
          reached.add(successor, successorPrecision);
          if (!newSuccessors.isEmpty()) {
            // There are some unhandled successors left, thus we re-add the old state into the
            // wait list.
            reached.reAddToWaitlist(state);
          }
          return true;
        }
      }
      assert action == Action.CONTINUE;

      Collection<AbstractState> matchedReached = reached.getReached(successor);
      if (mergeOperator != MergeSepOperator.getInstance() && !matchedReached.isEmpty()) {
        stats.mergeTimer.start();
        try {
          List<AbstractState> toRemove = new ArrayList<>();
          List<Pair<AbstractState, Precision>> toAdd = new ArrayList<>();
          for (AbstractState reachedState : matchedReached) {
            // use successor state to update reached state
            AbstractState mergedState = mergeOperator.merge(successor, reachedState,
                successorPrecision);
            if (!mergedState.equals(reachedState)) {
              // there is new information for reached state
              stats.countMerge++;
              toRemove.add(reachedState);
              toAdd.add(Pair.of(mergedState, successorPrecision));
            }
          }
          reached.removeAll(toRemove);
          reached.addAll(toAdd);
          if (mergeOperator instanceof ARGMergeJoinCPAEnabledAnalysis) {
            ((ARGMergeJoinCPAEnabledAnalysis) mergeOperator).cleanUp(reached);
          }
        } finally {
          stats.mergeTimer.stop();
        }
      }

      stats.stopTimer.start();
      boolean stop;
      try {
        stop = stopOperator.stop(successor, matchedReached, successorPrecision);
      } finally {
        stats.stopTimer.stop();
      }
      if (stop) {
        stats.countStop++;
      } else {
        reached.add(successor, successorPrecision);
      }
    }

    return false;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    if (forcedCovering instanceof StatisticsProvider) {
      ((StatisticsProvider) forcedCovering).collectStatistics(statsCollection);
    }
    statsCollection.add(stats);
  }

  @Override
  public void resetStatus() {
    stats.reset();
  }
}
