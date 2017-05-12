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
package org.sosy_lab.cpachecker.core.algorithm.summary;

import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.cpa.arg.ARGMergeJoinCPAEnabledAnalysis;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Compute summary use a CPA
 * It is required that each derived class of CPABasedSummaryComputer should have
 * a *PUBLIC* constructor in the form of
 *
 * XyzSummaryComputer(Configuration, LogManager, ShutdownNotifier)
 */
@Options
abstract public class CPABasedSummaryComputer extends DependencyBasedSummaryComputer {
  // CPA
  protected ConfigurableProgramAnalysis cpa;

  // Components of a CPA, cache them
  private final TransferRelation transferRelation;
  private final PrecisionAdjustment precisionAdjustment;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;

  protected CPABasedSummaryComputer(
      Configuration config,
      LogManager logger,
      ShutdownNotifier shutdownNotifier) throws InvalidConfigurationException {
    super(config, logger, shutdownNotifier);
    // create CPA
    cpa = createCPA(this.config, logger, shutdownNotifier);
    transferRelation = cpa == null ? null : cpa.getTransferRelation();
    precisionAdjustment = cpa == null ? null : cpa.getPrecisionAdjustment();
    mergeOperator = cpa == null ? null : cpa.getMergeOperator();
    stopOperator = cpa == null ? null : cpa.getStopOperator();
  }

  /**
   * Create CPA
   */
  abstract protected ConfigurableProgramAnalysis createCPA(
      Configuration config,
      LogManager logger,
      ShutdownNotifier shutdownNotifier)
      throws InvalidConfigurationException;

  /**
   * Initialize Reached Set
   */
  abstract protected ReachedSet initReachedSetForSubject(
      SummarySubject subject,
      ConfigurableProgramAnalysis cpa);

  /**
   * Compute summary for a subject using a CPA algorithm
   * Return a partial summary is the subset of summary entries that should be updated
   *
   * @return always not null
   * @throws Exception any kind of exception
   */
  protected Map<? extends SummarySubject, ? extends SummaryInstance> computeFor0(
      ReachedSet reachedSet, SummarySubject subject, SummaryInstance old) throws Exception {
    Map<? extends SummarySubject, ? extends SummaryInstance> partialSummary;
    while (reachedSet.hasWaitingState()) {
      final AbstractState state = reachedSet.popFromWaitlist();
      final Precision precision = reachedSet.getPrecision(state);

      // logger.log(Level.FINER, "Retrieved state from waitlist");
      try {
        if (handleState(state, precision, reachedSet)) {
          break;
        }
      } catch (Exception e) {
        // re-add the old state to the waitlist, there might be unhandled successors left
        // that otherwise would be forgotten (which would be unsound)
        reachedSet.reAddToWaitlist(state);
        throw e;
      }
    }
    partialSummary = summarize(subject, reachedSet, old);
    // always return non-null
    if (partialSummary == null) {
      partialSummary = Maps.newHashMap();
    }
    return partialSummary;
  }

  /**
   * Handle one state from the waitlist, i.e., produce successors etc.
   *
   * @param state      The abstract state that was taken out of the waitlist
   * @param precision  The precision for this abstract state.
   * @param reachedSet The reached set.
   * @return true if analysis should terminate, false if analysis should continue with next state
   */
  boolean handleState(
      final AbstractState state, final Precision precision, final ReachedSet reachedSet)
      throws CPAException, InterruptedException {
    // logger.log(Level.ALL, "Current state is", state, "with precision", precision);

    Collection<? extends AbstractState> successors;
    successors = transferRelation.getAbstractSuccessors(state,
        Lists.<AbstractState>newArrayList(),
        precision);

    logger.log(Level.FINER, "Current state has", successors.size(), "successors");

    for (AbstractState successor : Iterables.consumingIterable(successors)) {
      logger.log(Level.FINER, "Considering successor of current state");
      logger.log(Level.ALL, "Successor of", state, "\nis", successor);

      PrecisionAdjustmentResult precAdjustmentResult;
      Optional<PrecisionAdjustmentResult> precAdjustmentOptional =
          precisionAdjustment.prec(
              successor, precision, reachedSet, Functions.<AbstractState>identity(), successor);
      if (!precAdjustmentOptional.isPresent()) {
        continue;
      }
      precAdjustmentResult = precAdjustmentOptional.get();

      successor = precAdjustmentResult.abstractState();
      Precision successorPrecision = precAdjustmentResult.precision();
      Action action = precAdjustmentResult.action();

      if (action == Action.BREAK) {
        logger.log(Level.FINER, "Break signalled, CPAAlgorithm will stop.");

        // add the new state
        reachedSet.add(successor, successorPrecision);

        if (!successors.isEmpty()) {
          // re-add the old state to the waitlist, there are unhandled
          // successors left that otherwise would be forgotten
          reachedSet.reAddToWaitlist(state);
        }

        return true;
      }
      assert action == Action.CONTINUE : "Enum Action has unhandled values!";

      Collection<AbstractState> reached = reachedSet.getReached(successor);

      // An optimization, we don't bother merging if we know that the
      // merge operator won't do anything (i.e., it is merge-sep).
      if (mergeOperator != MergeSepOperator.getInstance() && !reached.isEmpty()) {
        List<AbstractState> toRemove = new ArrayList<>();
        List<Pair<AbstractState, Precision>> toAdd = new ArrayList<>();

        logger.log(
            Level.FINER, "Considering", reached.size(), "states from reached set for merge");
        for (AbstractState reachedState : reached) {
          AbstractState mergedState =
              mergeOperator.merge(successor, reachedState, successorPrecision);

          if (!mergedState.equals(reachedState)) {
            logger.log(Level.FINER, "Successor was merged with state from reached set");
            logger.log(
                Level.ALL, "Merged", successor, "\nand", reachedState, "\n-->", mergedState);

            toRemove.add(reachedState);
            toAdd.add(Pair.of(mergedState, successorPrecision));
          }
        }
        reachedSet.removeAll(toRemove);
        reachedSet.addAll(toAdd);

        if (mergeOperator instanceof ARGMergeJoinCPAEnabledAnalysis) {
          ((ARGMergeJoinCPAEnabledAnalysis) mergeOperator).cleanUp(reachedSet);
        }
      }

      boolean stop = stopOperator.stop(successor, reached, successorPrecision);

      if (stop) {
        logger.log(Level.FINER, "Successor is covered or unreachable, not adding to waitlist");
      } else {
        logger.log(Level.FINER, "No need to stop, adding successor to waitlist");
        reachedSet.add(successor, successorPrecision);
      }
    }
    return false;
  }

  @Override
  public Set<SummarySubject> computeFor(SummarySubject subject) throws Exception {

    SummaryInstance oldInstance = summary.get(subject);

    // Pick next state using strategy
    // BFS, DFS or top sort according to the configuration

    ReachedSet reached = initReachedSetForSubject(subject, cpa);
    Preconditions.checkNotNull(reached);

    Map<? extends SummarySubject, ? extends SummaryInstance> partialSummary =
        computeFor0(reached, subject, oldInstance);

    // clear the reached set (for GC)
    reached.clear();

    // update the set of summary entries that should be modified
    // collect their dependers (which will trigger re-computation for them)
    Set<SummarySubject> influenced = Sets.newHashSet();
    for (Map.Entry<? extends SummarySubject, ? extends SummaryInstance> entry : partialSummary
        .entrySet()) {
      SummarySubject s = entry.getKey();
      SummaryInstance inst = entry.getValue();
      if (update(s, inst)) {
        influenced.addAll(getDepender(s));
      }
    }

    return influenced;
  }
}
