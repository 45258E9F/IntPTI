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
package org.sosy_lab.cpachecker.cpa.bam;

import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;


class TimedReducer implements Reducer {

  final Timer reduceTime = new Timer();
  final Timer expandTime = new Timer();
  final Timer reducePrecisionTime = new Timer();
  final Timer expandPrecisionTime = new Timer();

  private final Reducer wrappedReducer;

  public TimedReducer(Reducer pWrappedReducer) {
    wrappedReducer = pWrappedReducer;
  }

  @Override
  public AbstractState getVariableReducedState(
      AbstractState pExpandedState, Block pContext,
      CFANode pCallNode) {

    reduceTime.start();
    try {
      return wrappedReducer.getVariableReducedState(pExpandedState, pContext, pCallNode);
    } finally {
      reduceTime.stop();
    }
  }

  @Override
  public AbstractState getVariableExpandedState(
      AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) {

    expandTime.start();
    try {
      return wrappedReducer.getVariableExpandedState(pRootState, pReducedContext, pReducedState);
    } finally {
      expandTime.stop();
    }
  }

  @Override
  public Object getHashCodeForState(AbstractState pElementKey, Precision pPrecisionKey) {
    return wrappedReducer.getHashCodeForState(pElementKey, pPrecisionKey);
  }

  @Override
  public Precision getVariableReducedPrecision(
      Precision pPrecision,
      Block pContext) {
    reducePrecisionTime.start();
    try {
      return wrappedReducer.getVariableReducedPrecision(pPrecision, pContext);
    } finally {
      reducePrecisionTime.stop();
    }
  }

  @Override
  public Precision getVariableExpandedPrecision(
      Precision rootPrecision,
      Block rootContext,
      Precision reducedPrecision) {
    expandPrecisionTime.start();
    try {
      return wrappedReducer
          .getVariableExpandedPrecision(rootPrecision, rootContext, reducedPrecision);
    } finally {
      expandPrecisionTime.stop();
    }

  }

  @Override
  public int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision) {
    return wrappedReducer.measurePrecisionDifference(pPrecision, pOtherPrecision);
  }

  @Override
  public AbstractState getVariableReducedStateForProofChecking(
      AbstractState pExpandedState, Block pContext,
      CFANode pCallNode) {
    return wrappedReducer
        .getVariableReducedStateForProofChecking(pExpandedState, pContext, pCallNode);

  }

  @Override
  public AbstractState getVariableExpandedStateForProofChecking(
      AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) {
    return wrappedReducer
        .getVariableExpandedStateForProofChecking(pRootState, pReducedContext, pReducedState);
  }

  @Override
  public AbstractState rebuildStateAfterFunctionCall(
      AbstractState rootState, AbstractState entryState,
      AbstractState expandedState, FunctionExitNode exitLocation) {
    return wrappedReducer
        .rebuildStateAfterFunctionCall(rootState, entryState, expandedState, exitLocation);
  }
}
