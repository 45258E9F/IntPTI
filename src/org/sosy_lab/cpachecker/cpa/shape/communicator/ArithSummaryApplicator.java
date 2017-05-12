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
package org.sosy_lab.cpachecker.cpa.shape.communicator;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.summary.instance.arith.ArithFunctionSummaryInstance;
import org.sosy_lab.cpachecker.core.summary.instance.arith.ArithLoopSummaryInstance;
import org.sosy_lab.cpachecker.core.summary.instance.arith.ast.LinearConstraint;
import org.sosy_lab.cpachecker.core.summary.instance.arith.ast.LinearVariable;
import org.sosy_lab.cpachecker.core.summary.instance.arith.convert.LinearConstraintUtil;
import org.sosy_lab.cpachecker.core.summary.instance.arith.convert.SymbolicValueResolver;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicExpression;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class ArithSummaryApplicator {

  public static Collection<ShapeState> applyFunctionSummary(
      ShapeState pState,
      List<AbstractState> pOtherStates,
      CFunctionReturnEdge pEdge,
      ArithFunctionSummaryInstance pInstance) {
    List<LinearConstraint<Integer, LinearVariable>> constraints = pInstance.apply();
    SymbolicValueResolver svr = new SymbolicValueResolver(pState);
    ShapeState newState = new ShapeState(pState);
    for (SymbolicExpression c : LinearConstraintUtil.convert(constraints, svr)) {
      newState.addConstraint(c);
    }
    return Collections.singletonList(newState);
  }

  public static Multimap<CFAEdge, ShapeState> applyExternalLoopSummary(
      Multimap<CFAEdge, ShapeState> pEdge2States,
      List<AbstractState> pOtherStates,
      CFAEdge pInEdge,
      ArithLoopSummaryInstance pInstance) {
    ImmutableMultimap.Builder<CFAEdge, ShapeState> builder = ImmutableMultimap.builder();
    for (CFAEdge edge : pEdge2States.keySet()) {
      List<LinearConstraint<Integer, LinearVariable>> constraints = pInstance.apply(pInEdge, edge);
      Collection<ShapeState> states = pEdge2States.get(edge);
      for (ShapeState state : states) {
        SymbolicValueResolver svr = new SymbolicValueResolver(state);
        ShapeState newState = new ShapeState(state);
        for (SymbolicExpression c : LinearConstraintUtil.convert(constraints, svr)) {
          newState.addConstraint(c);
        }
        builder.put(edge, newState);
      }
    }
    return builder.build();
  }

  public static Collection<ShapeState> applyInternalLoopSummary(
      ShapeState pState,
      List<AbstractState> pOtherStates,
      CFAEdge pInEdge,
      ArithLoopSummaryInstance pInstance)
      throws CPATransferException {
    SymbolicValueResolver svr = new SymbolicValueResolver(pState);
    ShapeState newState = new ShapeState(pState);
    List<LinearConstraint<Integer, LinearVariable>> constraints = pInstance.apply(pInEdge);
    for (SymbolicExpression c : LinearConstraintUtil.convert(constraints, svr)) {
      newState.addConstraint(c);
    }
    return Collections.singletonList(newState);
  }
}
