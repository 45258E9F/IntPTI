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

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;


public class ARGReducer implements Reducer {

  private final Reducer wrappedReducer;

  public ARGReducer(Reducer pWrappedReducer) {
    wrappedReducer = pWrappedReducer;
  }

  @Override
  public AbstractState getVariableReducedState(
      AbstractState pExpandedState, Block pContext,
      CFANode pLocation) {

    return new ARGState(wrappedReducer
        .getVariableReducedState(((ARGState) pExpandedState).getWrappedState(), pContext,
            pLocation), null);
  }

  @Override
  public AbstractState getVariableExpandedState(
      AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) {

    return new ARGState(
        wrappedReducer.getVariableExpandedState(((ARGState) pRootState).getWrappedState(),
            pReducedContext, ((ARGState) pReducedState).getWrappedState()), null);
  }

  @Override
  public Object getHashCodeForState(AbstractState pElementKey, Precision pPrecisionKey) {

    return wrappedReducer
        .getHashCodeForState(((ARGState) pElementKey).getWrappedState(), pPrecisionKey);
  }

  @Override
  public Precision getVariableReducedPrecision(
      Precision pPrecision,
      Block pContext) {
    return wrappedReducer.getVariableReducedPrecision(pPrecision, pContext);
  }

  @Override
  public Precision getVariableExpandedPrecision(
      Precision rootPrecision,
      Block rootContext,
      Precision reducedPrecision) {
    return wrappedReducer
        .getVariableExpandedPrecision(rootPrecision, rootContext, reducedPrecision);
  }

  @Override
  public int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision) {
    return wrappedReducer.measurePrecisionDifference(pPrecision, pOtherPrecision);
  }

  @Override
  public AbstractState getVariableReducedStateForProofChecking(
      AbstractState pExpandedState, Block pContext,
      CFANode pCallNode) {
    return new ARGState(wrappedReducer.getVariableReducedStateForProofChecking(
        ((ARGState) pExpandedState).getWrappedState(), pContext, pCallNode), null);
  }

  @Override
  public AbstractState getVariableExpandedStateForProofChecking(
      AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) {
    return new ARGState(wrappedReducer.getVariableExpandedStateForProofChecking(
        ((ARGState) pRootState).getWrappedState(), pReducedContext,
        ((ARGState) pReducedState).getWrappedState()), null);
  }

  @Override
  public AbstractState rebuildStateAfterFunctionCall(
      AbstractState rootState, AbstractState entryState,
      AbstractState expandedState, FunctionExitNode exitLocation) {
    return new ARGState(
        wrappedReducer.rebuildStateAfterFunctionCall(
            ((ARGState) rootState).getWrappedState(),
            ((ARGState) entryState).getWrappedState(),
            ((ARGState) expandedState).getWrappedState(),
            exitLocation),
        null);
  }
}
