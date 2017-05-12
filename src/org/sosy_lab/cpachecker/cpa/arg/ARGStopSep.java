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
package org.sosy_lab.cpachecker.cpa.arg;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCoveringStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;

@Options(prefix = "cpa.arg")
public class ARGStopSep implements StopOperator, ForcedCoveringStopOperator {

  @Option(secure = true, description = "whether to keep covered states in the reached set as addition to keeping them in the ARG")
  private boolean keepCoveredStatesInReached = false;

  @Option(secure = true,
      description = "inform ARG CPA if it is run in a CPA enabled analysis because then it must"
          + "behave differently during merge.")
  private boolean inCPAEnabledAnalysis = false;

  private final StopOperator wrappedStop;
  private final LogManager logger;

  public ARGStopSep(StopOperator pWrappedStop, LogManager pLogger, Configuration config)
      throws InvalidConfigurationException {
    config.inject(this);
    wrappedStop = pWrappedStop;
    logger = pLogger;
  }

  @Override
  public boolean stop(
      AbstractState pElement,
      Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {

    ARGState argElement = (ARGState) pElement;
    assert !argElement.isCovered() : "Passing element to stop which is already covered: "
        + argElement;

    // First check if we can take a shortcut:
    // If the new state was merged into an existing element,
    // it is usually also covered by this existing element, so check this explicitly upfront.
    // We do this because we want to remove the new state from the ARG completely
    // in this case and not mark it as covered.

    if (argElement.getMergedWith() != null) {
      ARGState mergedWith = argElement.getMergedWith();

      if (pReached.contains(mergedWith)) {
        // we do this single check first as it should return true in most of the cases

        if (wrappedStop
            .stop(argElement.getWrappedState(), Collections.singleton(mergedWith.getWrappedState()),
                pPrecision)) {
          // merged and covered
          if (inCPAEnabledAnalysis) {
            argElement.setCovered(mergedWith);
          } else {
            argElement.removeFromARG();
          }
          logger.log(Level.FINEST, "Element is covered by the element it was merged into");
          // After state merge, we should decrement the path counter by 1.
          ARGPathCounter.dec(1);

          // in this case, return true even if we should keep covered states
          // because we should anyway not keep merged states
          return true;

        } else {
          // unexpected case, but possible (if merge does not compute the join, but just widens e2)
          logger.log(Level.FINEST, "Element was merged but not covered:", pElement);
        }

      } else {
        // unexpected case, not sure if it this possible
        logger.log(Level.FINEST,
            "Element was merged into an element that's not in the reached set, merged-with element is",
            mergedWith);
      }
    }

    // Never try to cover target states
    if (argElement.isTarget()) {
      return false;
    }

    // Now do the usual coverage checks

    for (AbstractState reachedState : pReached) {
      ARGState argReachedState = (ARGState) reachedState;
      if (stop(argElement, argReachedState, pPrecision)) {
        // if this option is true, we always return false here on purpose
        return !keepCoveredStatesInReached;
      }
    }
    return false;

  }

  private boolean stop(ARGState pElement, ARGState pReachedState, Precision pPrecision)
      throws CPAException, InterruptedException {

    if (!pReachedState.mayCover()) {
      return false;
    }
    if (pElement == pReachedState) {
      return false;
    }
    if (pElement.isOlderThan(pReachedState)) {
      // This is never the case in usual predicate abstraction,
      // but possibly with other algorithms
      // Checking this also implies that pElement gets not covered by
      // one of its children (because they are all newer than pElement).
      return false;
    }

    AbstractState wrappedState = pElement.getWrappedState();
    AbstractState wrappedReachedState = pReachedState.getWrappedState();

    boolean stop =
        wrappedStop.stop(wrappedState, Collections.singleton(wrappedReachedState), pPrecision);

    if (stop) {
      pElement.setCovered(pReachedState);
      // If a state is being covered by another existing state, such state should not have its
      // own successor, and thus we should decrement the path counter.
      ARGPathCounter.dec(1);
    }
    return stop;
  }

  boolean isCoveredBy(
      AbstractState pElement,
      AbstractState pOtherElement,
      ProofChecker wrappedProofChecker) throws CPAException, InterruptedException {
    ARGState argElement = (ARGState) pElement;
    ARGState otherArtElement = (ARGState) pOtherElement;

    AbstractState wrappedState = argElement.getWrappedState();
    AbstractState wrappedOtherElement = otherArtElement.getWrappedState();

    return wrappedProofChecker.isCoveredBy(wrappedState, wrappedOtherElement);
  }

  @Override
  public boolean isForcedCoveringPossible(
      AbstractState pElement,
      AbstractState pReachedState,
      Precision pPrecision) throws CPAException, InterruptedException {
    if (!(wrappedStop instanceof ForcedCoveringStopOperator)) {
      return false;
    }

    ARGState element = (ARGState) pElement;
    ARGState reachedState = (ARGState) pReachedState;

    if (reachedState.isCovered() || !reachedState.mayCover()) {
      return false;
    }

    if (element.isOlderThan(reachedState)) {
      return false;
    }

    return ((ForcedCoveringStopOperator) wrappedStop).isForcedCoveringPossible(
        element.getWrappedState(), reachedState.getWrappedState(), pPrecision);
  }
}
