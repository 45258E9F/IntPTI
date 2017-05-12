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
package org.sosy_lab.cpachecker.cpa.bounds;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BoundsTransferRelation extends SingleEdgeTransferRelation {

  private Multimap<CFANode, Loop> loopHeads = null;

  private final int maxLoopIterations;
  private final int maxRecursionDepth;
  private final int loopIterationsBeforeAbstraction;

  public BoundsTransferRelation(
      int pLoopIterationsBeforeAbstraction,
      int pMaxLoopIterations,
      int pMaxRecursionDepth,
      LoopStructure pLoops) {

    loopIterationsBeforeAbstraction = pLoopIterationsBeforeAbstraction;
    this.maxLoopIterations = pMaxLoopIterations;
    this.maxRecursionDepth = pMaxRecursionDepth;

    ImmutableMultimap.Builder<CFANode, Loop> heads = ImmutableMultimap.builder();

    for (Loop l : pLoops.getAllLoops()) {
      for (CFANode h : l.getLoopHeads()) {
        heads.put(h, l);
      }
    }
    loopHeads = heads.build();
  }


  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pElement,
      List<AbstractState> otherStates,
      Precision pPrecision,
      CFAEdge pCfaEdge)
      throws CPATransferException {

    BoundsState e = (BoundsState) pElement;

    if (e.isStopState()) {
      return Collections.emptySet();
    }

    if (pCfaEdge instanceof FunctionCallEdge) {
      // such edges do never change loop stack status
      return Collections.singleton(pElement);
    }

    if (pCfaEdge instanceof FunctionReturnEdge) {
      e = e.returnFromFunction();
    }

    CFANode loc = pCfaEdge.getSuccessor();

    Collection<Loop> loops = loopHeads.get(loc);
    assert loops.size() <= 1;
    if (!loops.isEmpty()) {
      for (Loop loop : loops) {
        e = e.enter(loop, loopIterationsBeforeAbstraction);
      }
      if ((maxLoopIterations > 0)
          && e.getDeepestIteration() > maxLoopIterations) {
        e = e.stopIt();
      }
    }

    if (maxRecursionDepth > 0 && e.getDeepestRecursion() > maxRecursionDepth) {
      e = e.stopRec();
    }

    return Collections.singleton(e);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) {

    BoundsState state = (BoundsState) pState;

    for (CallstackState callstackState : FluentIterable.from(pOtherStates)
        .filter(CallstackState.class)) {
      int recursionDepth = getRecursionDepth(callstackState);
      if (recursionDepth > state.getDeepestRecursion()) {
        assert recursionDepth == getDeepestRecursion(callstackState);
        state = state.setDeepestRecursion(recursionDepth);
      }
      state = state.setCurrentFunction(callstackState.getCurrentFunction());
      if (state.getReturnFromCounter() > state.getDeepestRecursion()) {
        state = state.setDeepestRecursion(state.getReturnFromCounter());
      }
    }
    return state.equals(pState) ? null : Collections.singleton(state);
  }

  private static final int getRecursionDepth(CallstackState pCallstackState) {
    int depth = 0;
    CallstackState state = pCallstackState;
    String function = pCallstackState.getCurrentFunction();
    while (state != null) {
      if (state.getCurrentFunction().equals(function)) {
        ++depth;
      }
      state = state.getPreviousState();
    }
    return depth;
  }

  private static final int getDeepestRecursion(CallstackState pCallstackState) {
    Map<String, Integer> depths = Maps.newHashMap();
    CallstackState state = pCallstackState;
    int deepest = 0;
    while (state != null) {
      String function = state.getCurrentFunction();
      Integer currentDepth = depths.get(function);
      currentDepth = currentDepth == null ? 1 : currentDepth + 1;
      if (currentDepth > deepest) {
        deepest = currentDepth;
      }
      depths.put(function, currentDepth);
      state = state.getPreviousState();
    }
    return deepest;
  }
}