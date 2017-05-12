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
package org.sosy_lab.cpachecker.cpa.composite;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.Iterator;

/**
 * Provides a MergeOperator implementation that just delegates to the component
 * CPAs without any further logic.
 */
public class CompositeMergePlainOperator implements MergeOperator {

  private final ImmutableList<MergeOperator> mergeOperators;

  public CompositeMergePlainOperator(ImmutableList<MergeOperator> mergeOperators) {
    this.mergeOperators = mergeOperators;
  }

  @Override
  public AbstractState merge(
      AbstractState successorState,
      AbstractState reachedState,
      Precision precision) throws CPAException, InterruptedException {

    // Merge Sep Code
    CompositeState compSuccessorState = (CompositeState) successorState;
    CompositeState compReachedState = (CompositeState) reachedState;
    CompositePrecision compPrecision = (CompositePrecision) precision;

    assert (compSuccessorState.getNumberOfStates() == compReachedState.getNumberOfStates());

    ImmutableList.Builder<AbstractState> mergedStates = ImmutableList.builder();
    Iterator<AbstractState> iter1 = compSuccessorState.getWrappedStates().iterator();
    Iterator<AbstractState> iter2 = compReachedState.getWrappedStates().iterator();
    Iterator<Precision> iterPrec = compPrecision.getPrecisions().iterator();

    boolean identicalStates = true;
    for (MergeOperator mergeOp : mergeOperators) {
      AbstractState absSuccessorState = iter1.next();
      AbstractState absReachedState = iter2.next();
      AbstractState mergedState =
          mergeOp.merge(absSuccessorState, absReachedState, iterPrec.next());

      if (mergedState != absReachedState) {
        identicalStates = false;
      }
      mergedStates.add(mergedState);
    }

    if (identicalStates) {
      return reachedState;
    } else {
      return new CompositeState(mergedStates.build());
    }
  }
}
