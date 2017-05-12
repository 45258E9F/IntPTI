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
package org.sosy_lab.cpachecker.cpa.callstack;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;


public class CallstackReducer implements Reducer {

  @Override
  public AbstractState getVariableReducedState(
      AbstractState pExpandedState, Block pContext, CFANode callNode) {

    CallstackState element = (CallstackState) pExpandedState;

    return copyCallstackUpToCallNode(element, callNode);
    //    return new CallstackState(null, state.getCurrentFunction(), location);
  }

  private CallstackState copyCallstackUpToCallNode(CallstackState element, CFANode callNode) {
    if (element.getCurrentFunction().equals(callNode.getFunctionName())) {
      return new CallstackState(null, element.getCurrentFunction(), callNode);
    } else {
      assert element.getPreviousState() != null;
      CallstackState recursiveResult =
          copyCallstackUpToCallNode(element.getPreviousState(), callNode);
      return new CallstackState(recursiveResult,
          element.getCurrentFunction(),
          element.getCallNode());
    }
  }

  @Override
  public AbstractState getVariableExpandedState(
      AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) {

    CallstackState rootState = (CallstackState) pRootState;
    CallstackState reducedState = (CallstackState) pReducedState;

    // the stackframe on top of rootState and the stackframe on bottom of reducedState are the same function
    // now glue both stacks together at this state

    return copyCallstackExceptLast(rootState, reducedState);
  }

  private CallstackState copyCallstackExceptLast(CallstackState target, CallstackState source) {
    if (source.getDepth() == 1) {
      assert source.getPreviousState() == null;
      assert source.getCurrentFunction().equals(target.getCurrentFunction()) :
          "names of functions do not match: '" + source.getCurrentFunction() + "' != '" + target
              .getCurrentFunction() + "'";
      return target;
    } else {
      CallstackState recursiveResult = copyCallstackExceptLast(target, source.getPreviousState());

      return new CallstackState(
          recursiveResult,
          source.getCurrentFunction(),
          source.getCallNode());
    }
  }

  @Override
  public Object getHashCodeForState(AbstractState pElementKey, Precision pPrecisionKey) {
    return new CallstackStateEqualsWrapper((CallstackState) pElementKey);
  }

  @Override
  public Precision getVariableReducedPrecision(
      Precision pPrecision,
      Block pContext) {
    return pPrecision;
  }

  @Override
  public Precision getVariableExpandedPrecision(
      Precision rootPrecision,
      Block rootContext,
      Precision reducedPrecision) {
    return reducedPrecision;
  }

  @Override
  public int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision) {
    return 0;
  }

  @Override
  public AbstractState getVariableReducedStateForProofChecking(
      AbstractState pExpandedState, Block pContext,
      CFANode pCallNode) {
    return getVariableReducedState(pExpandedState, pContext, pCallNode);
  }

  @Override
  public AbstractState getVariableExpandedStateForProofChecking(
      AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) {
    return getVariableExpandedState(pRootState, pReducedContext, pReducedState);
  }

  @Override
  public AbstractState rebuildStateAfterFunctionCall(
      AbstractState rootState, AbstractState entryState,
      AbstractState expandedState, FunctionExitNode exitLocation) {
    return expandedState;
  }
}
