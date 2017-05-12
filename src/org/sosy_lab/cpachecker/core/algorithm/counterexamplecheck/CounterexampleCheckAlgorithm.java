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
package org.sosy_lab.cpachecker.core.algorithm.counterexamplecheck;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.ImmutableList.copyOf;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.toPercent;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

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
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;

@Options(prefix = "counterexample")
public class CounterexampleCheckAlgorithm implements Algorithm, StatisticsProvider, Statistics {

  enum CounterexampleCheckerType {
    CBMC,
    CPACHECKER,
    CONCRETE_EXECUTION;
  }

  private final Algorithm algorithm;
  private final CounterexampleChecker checker;
  private final LogManager logger;
  private final ARGCPA cpa;

  private final Timer checkTime = new Timer();
  private int numberOfInfeasiblePaths = 0;

  private final Set<ARGState> checkedTargetStates =
      Collections.newSetFromMap(new WeakHashMap<ARGState, Boolean>());

  @Option(secure = true, name = "checker",
      description = "Which model checker to use for verifying counterexamples as a second check.\n"
          + "Currently CBMC or CPAchecker with a different config or the concrete execution \n"
          + "checker can be used.")
  private CounterexampleCheckerType checkerType = CounterexampleCheckerType.CBMC;

  @Option(secure = true, description = "continue analysis after an counterexample was found that was denied by the second check")
  private boolean continueAfterInfeasibleError = true;

  @Option(secure = true, description =
      "If continueAfterInfeasibleError is true, remove the infeasible counterexample before continuing."
          + "Setting this to false may prevent a lot of similar infeasible counterexamples to get discovered, but is unsound")
  private boolean removeInfeasibleErrors = false;

  public CounterexampleCheckAlgorithm(
      Algorithm algorithm,
      ConfigurableProgramAnalysis pCpa, Configuration config, LogManager logger,
      ShutdownNotifier pShutdownNotifier, CFA cfa, String filename)
      throws InvalidConfigurationException {
    this.algorithm = algorithm;
    this.logger = logger;
    config.inject(this);

    if (!(pCpa instanceof ARGCPA)) {
      throw new InvalidConfigurationException("ARG CPA needed for counterexample check");
    }
    cpa = (ARGCPA) pCpa;

    switch (checkerType) {
      case CBMC:
        checker = new CBMCChecker(config, logger, cfa);
        break;
      case CPACHECKER:
        checker =
            new CounterexampleCPAChecker(config, logger, pShutdownNotifier, cfa, filename, cpa);
        break;
      case CONCRETE_EXECUTION:
        checker = new ConcretePathExecutionChecker(config, logger, cfa, cpa);
        break;
      default:
        throw new AssertionError("Unhandled case statement: " + checkerType);
    }
  }

  @Override
  public AlgorithmStatus run(ReachedSet reached) throws CPAException, InterruptedException {
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;

    while (reached.hasWaitingState()) {
      status = status.update(algorithm.run(reached));
      assert ARGUtils.checkARG(reached);

      ARGState lastState = (ARGState) reached.getLastState();

      Deque<ARGState> errorStates = new ArrayDeque<>();
      if (lastState != null && lastState.isTarget()) {
        errorStates.add(lastState);
      } else {
        from(reached)
            .transform(AbstractStates.toState(ARGState.class))
            .filter(AbstractStates.IS_TARGET_STATE)
            .filter(Predicates.not(Predicates.in(checkedTargetStates)))
            .copyInto(errorStates);
      }

      if (errorStates.isEmpty()) {
        // no errors, so no analysis necessary
        break;
      }

      // check counterexample
      checkTime.start();
      try {
        boolean foundCounterexample = false;
        while (!errorStates.isEmpty()) {
          ARGState errorState = errorStates.pollFirst();
          if (!reached.contains(errorState)) {
            // errorState was already removed due to earlier loop iterations
            continue;
          }

          status = AlgorithmStatus.SOUND_AND_PRECISE.withSound(
              checkCounterexample(errorState, reached, status.isSound()));
          if (reached.contains(errorState)) {
            checkedTargetStates.add(errorState);
            foundCounterexample = true;
          }
        }

        if (foundCounterexample) {
          break;
        }
      } finally {
        checkTime.stop();
      }
    }
    return status;
  }

  private boolean checkCounterexample(
      ARGState errorState, ReachedSet reached,
      boolean sound) throws InterruptedException, CPAException, RefinementFailedException {
    ARGState rootState = (ARGState) reached.getFirstState();

    Set<ARGState> statesOnErrorPath = ARGUtils.getAllStatesOnPathsTo(errorState);

    logger.log(Level.INFO,
        "Error path found, starting counterexample check with " + checkerType + ".");
    boolean feasibility;
    try {
      feasibility = checker.checkCounterexample(rootState, errorState, statesOnErrorPath);
    } catch (CPAException e) {
      logger.logUserException(Level.WARNING, e,
          "Counterexample found, but feasibility could not be verified");
      //return false;
      throw e;
    }

    if (feasibility) {
      logger.log(Level.INFO,
          "Error path found and confirmed by counterexample check with " + checkerType + ".");
      return sound;

    } else {
      numberOfInfeasiblePaths++;
      logger.log(Level.INFO,
          "Error path found, but identified as infeasible by counterexample check with "
              + checkerType + ".");

      if (continueAfterInfeasibleError) {
        // This counterexample is infeasible, so usually we would remove it
        // from the reached set. This is not possible, because the
        // counterexample of course contains the root state and we don't
        // know up to which point we have to remove the path from the reached set.
        // However, we also cannot let it stay in the reached set, because
        // then the states on the path might cover other, actually feasible,
        // paths, so this would prevent other real counterexamples to be found (unsound!).

        // So there are two options: either let them stay in the reached set
        // and mark analysis as unsound, or let them stay in the reached set
        // and prevent them from covering new paths.

        if (removeInfeasibleErrors) {
          sound &= handleInfeasibleCounterexample(reached, statesOnErrorPath);
        } else if (sound) {
          logger.log(Level.WARNING,
              "Infeasible counterexample found, but could not remove it from the ARG. Therefore, we cannot prove safety.");
          sound = false;
        }

        sound &= removeErrorState(reached, errorState);
        assert ARGUtils.checkARG(reached);

      } else {
        ARGPath path = ARGUtils.getOnePathTo(errorState);
        throw new RefinementFailedException(Reason.InfeasibleCounterexample, path);
      }
    }
    return sound;
  }

  private boolean handleInfeasibleCounterexample(
      ReachedSet reached,
      Set<ARGState> statesOnErrorPath) {
    boolean sound = true;

    // So we let the states stay in the reached set, and just prevent
    // them from covering other states by removing all existing
    // coverage relations (and re-adding the covered states)
    // and preventing new ones via ARGState#setNotCovering().

    Collection<ARGState> coveredByErrorPath = new ArrayList<>();

    for (ARGState errorPathState : statesOnErrorPath) {
      // schedule for coverage removal
      coveredByErrorPath.addAll(errorPathState.getCoveredByThis());

      // prevent future coverage
      errorPathState.setNotCovering();
    }

    for (ARGState coveredState : coveredByErrorPath) {
      if (isTransitiveChildOf(coveredState, coveredState.getCoveringState())) {
        // This state is covered by one of it's (transitive) parents
        // so this is a loop.
        // Don't add the state, because otherwise the loop would
        // get unrolled endlessly.
        logger.log(Level.WARNING,
            "Infeasible counterexample found, but could not remove it from the ARG due to loops in the counterexample path. Therefore, we cannot prove safety.");
        sound = false;
        continue;
      }

      for (ARGState parentOfCovered : coveredState.getParents()) {
        if (statesOnErrorPath.contains(parentOfCovered)) {
          // this should never happen, but handle anyway
          // we may not re-add this parent, because otherwise
          // the error-path will be re-discovered again
          // but not adding the parent is unsound
          logger.log(Level.WARNING,
              "Infeasible counterexample found, but could not remove it from the ARG. Therefore, we cannot prove safety.");
          sound = false;

        } else {
          // let covered state be re-discovered
          reached.reAddToWaitlist(parentOfCovered);
        }
      }
      assert !reached.contains(coveredState) : "covered state in reached set";
      coveredState.removeFromARG();
    }
    return sound;
  }

  private boolean isTransitiveChildOf(ARGState potentialChild, ARGState potentialParent) {

    Set<ARGState> seen = new HashSet<>();
    Deque<ARGState> waitlist = new ArrayDeque<>(); // use BFS

    waitlist.addAll(potentialChild.getParents());
    while (!waitlist.isEmpty()) {
      ARGState current = waitlist.pollFirst();

      for (ARGState currentParent : current.getParents()) {
        if (currentParent.equals(potentialParent)) {
          return true;
        }

        if (!seen.add(currentParent)) {
          waitlist.addLast(currentParent);
        }
      }
    }

    return false;
  }

  private boolean removeErrorState(ReachedSet reached, ARGState errorState) {
    boolean sound = true;

    assert errorState.getChildren().isEmpty();
    assert errorState.getCoveredByThis().isEmpty();

    // remove re-added parent of errorState to prevent computing
    // the same error state over and over
    Collection<ARGState> parents = errorState.getParents();
    assert parents.size() == 1 : "error state that was merged";

    ARGState parent = Iterables.getOnlyElement(parents);

    if (parent.getChildren().size() > 1 || parent.getCoveredByThis().isEmpty()) {
      // The error state has a sibling, so the parent and the sibling
      // should stay in the reached set, but then the error state
      // would get re-discovered.
      // Similarly for covered states.
      // Currently just handle this by removing them anyway,
      // as this probably doesn't occur.
      sound = false;
    }

    // this includes the errorState and its siblings
    List<ARGState> siblings = copyOf(parent.getChildren());
    for (ARGState toRemove : siblings) {

      assert toRemove.getChildren().isEmpty();

      // state toRemove may cover some states, but hopefully only siblings which we remove anyway
      assert siblings.containsAll(toRemove.getCoveredByThis());

      reached.remove(toRemove);
      toRemove.removeFromARG();
    }

    List<ARGState> coveredByParent = copyOf(parent.getCoveredByThis());
    for (ARGState covered : coveredByParent) {
      assert covered.getChildren().isEmpty();
      assert covered.getCoveredByThis().isEmpty();

      // covered is not in reached
      covered.removeFromARG();
    }

    cpa.clearCounterexamples(ImmutableSet.of(errorState));
    reached.remove(parent);
    parent.removeFromARG();

    assert errorState.isDestroyed() : "errorState is not the child of its parent";
    assert !reached.contains(errorState) : "reached.remove() didn't work";
    return sound;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) algorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(this);
  }

  @Override
  public void printStatistics(
      PrintStream out, Result pResult,
      ReachedSet pReached) {

    out.println("Number of counterexample checks:    " + checkTime.getNumberOfIntervals());
    if (checkTime.getNumberOfIntervals() > 0) {
      out.println(
          "Number of infeasible paths:         " + numberOfInfeasiblePaths + " (" + toPercent(
              numberOfInfeasiblePaths, checkTime.getNumberOfIntervals()) + ")");
      out.println("Time for counterexample checks:     " + checkTime);
      if (checker instanceof Statistics) {
        ((Statistics) checker).printStatistics(out, pResult, pReached);
      }
    }
  }

  @Override
  public String getName() {
    return "Counterexample-Check Algorithm";
  }
}
