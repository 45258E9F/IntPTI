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
package org.sosy_lab.cpachecker.cpa.arg;

import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.merge.HybridMergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.merge.MergeMode;
import org.sosy_lab.cpachecker.core.interfaces.merge.MergeTactic;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.List;

public class ARGHybridMerge implements HybridMergeOperator {

  private final MergeOperator wrappedMerge;
  private MergeMode mergeMode;

  public ARGHybridMerge(MergeOperator pMerge) {
    wrappedMerge = pMerge;
    mergeMode = MergeMode.SEP;
  }

  @Override
  public MergeMode getMergeMode() {
    return mergeMode;
  }

  @Override
  public AbstractState merge(
      AbstractState state1, AbstractState state2, Precision precision)
      throws CPAException, InterruptedException {
    // first of all, we should reset the working mode of merge operator
    resetMode();
    ARGState argState1 = (ARGState) state1;
    ARGState argState2 = (ARGState) state2;
    if (wrappedMerge == MergeSepOperator.getInstance()) {
      return argState2;
    }

    assert (!argState1.isCovered()) : "trying to merge covered state: " + argState1;
    if (!argState2.mayCover()) {
      // equivalent to "SEP" merge
      return argState2;
    }
    if (argState1.getMergedWith() != null) {
      return argState2;
    }

    AbstractState wrappedState1 = argState1.getWrappedState();
    AbstractState wrappedState2 = argState2.getWrappedState();
    AbstractState mergedState = wrappedMerge.merge(wrappedState1, wrappedState2, precision);
    if (wrappedMerge instanceof HybridMergeOperator) {
      MergeMode wrappedMode = ((HybridMergeOperator) wrappedMerge).getMergeMode();
      if (wrappedMode == MergeMode.SEP) {
        return argState2;
      } else {
        assert (wrappedMode == MergeMode.JOIN);
      }
    }
    if (mergedState.equals(wrappedState2)) {
      return argState2;
    }
    // if we reach here, then one of the following holds: (1) the wrapped merge operator perform
    // join-merge; (2) the wrapped merge operator is hybrid and works in the JOIN mode
    mergeMode = MergeMode.JOIN;
    ARGState newState = new ARGState(mergedState, null);
    // replace the existing reached state
    argState2.replaceInARGWith(newState);
    for (ARGState parentOfState1 : argState1.getParents()) {
      newState.addParent(parentOfState1);
    }
    // sanity check: state1 is the new derived successor state
    assert argState1.getChildren().isEmpty();
    assert argState1.getCoveredByThis().isEmpty();
    argState1.setMergedWith(newState);
    return newState;
  }

  /**
   * Reset the merging mode.
   */
  private void resetMode() {
    mergeMode = MergeMode.SEP;
  }

  @Override
  public AbstractState merge(
      AbstractState state1,
      List<AbstractState> otherStates1,
      AbstractState state2,
      List<AbstractState> otherStates2,
      Precision precision,
      List<Precision> otherPrecisions) throws CPAException, InterruptedException {
    throw new UnsupportedOperationException("compound merge is not supported in ARG CPA");
  }

  @Override
  public void addTactics(List<MergeTactic> tactics) {
    throw new UnsupportedOperationException("tactic sharing is not supported in ARG CPA");
  }
}
