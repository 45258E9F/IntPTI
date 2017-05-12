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
package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.delegation;

import com.google.common.base.Optional;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisStrongestPostOperator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.ForgettingCompositeState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.SymbolicStrongestPostOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Strongest-post operator with the semantics of
 * {@link org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisStrongestPostOperator
 * ValueAnalysisStrongestPostOperator},
 * but using {@link ForgettingCompositeState} as state type.
 */
public class DelegatingStrongestPost implements SymbolicStrongestPostOperator {

  private static final ConstraintsState INITIAL_CONSTRAINTS = new ConstraintsState();

  private final ValueAnalysisStrongestPostOperator explicitStrongestPost;

  public DelegatingStrongestPost(
      final LogManager pLogger,
      final Configuration pConfig,
      final CFA pCfa
  ) throws InvalidConfigurationException {
    explicitStrongestPost =
        new ValueAnalysisStrongestPostOperator(pLogger, pConfig, pCfa);
  }

  @Override
  public Optional<ForgettingCompositeState> getStrongestPost(
      final ForgettingCompositeState pOrigin,
      final Precision pPrecision,
      final CFAEdge pOperation
  ) throws CPAException {
    Optional<ValueAnalysisState> successor =
        explicitStrongestPost.getStrongestPost(pOrigin.getValueState(), pPrecision, pOperation);

    if (!successor.isPresent()) {
      return Optional.absent();
    } else {
      ValueAnalysisState next = successor.get();
      return Optional.of(new ForgettingCompositeState(next, INITIAL_CONSTRAINTS));
    }
  }

  @Override
  public ForgettingCompositeState handleFunctionCall(
      final ForgettingCompositeState pState,
      final CFAEdge pEdge,
      final Deque<ForgettingCompositeState> pCallstack
  ) {
    Deque<ValueAnalysisState> valueCallstack = transformToValueStack(pCallstack);

    ValueAnalysisState result =
        explicitStrongestPost.handleFunctionCall(pState.getValueState(), pEdge, valueCallstack);

    return new ForgettingCompositeState(result, INITIAL_CONSTRAINTS);
  }

  private Deque<ValueAnalysisState> transformToValueStack(
      final Deque<ForgettingCompositeState> pCallstack
  ) {
    Deque<ValueAnalysisState> valueCallstack = new ArrayDeque<>(pCallstack.size());

    for (ForgettingCompositeState s : pCallstack) {
      valueCallstack.add(s.getValueState());
    }

    return valueCallstack;
  }

  @Override
  public ForgettingCompositeState handleFunctionReturn(
      final ForgettingCompositeState pNext,
      final CFAEdge pEdge,
      final Deque<ForgettingCompositeState> pCallstack
  ) {
    Deque<ValueAnalysisState> valueCallstack = transformToValueStack(pCallstack);
    ValueAnalysisState result =
        explicitStrongestPost.handleFunctionReturn(pNext.getValueState(), pEdge, valueCallstack);

    return new ForgettingCompositeState(result, INITIAL_CONSTRAINTS);
  }

  @Override
  public ForgettingCompositeState performAbstraction(
      final ForgettingCompositeState pNext,
      final CFANode pCurrNode,
      final ARGPath pErrorPath,
      final Precision pPrecision
  ) {
    ValueAnalysisState result = explicitStrongestPost.performAbstraction(
        pNext.getValueState(), pCurrNode, pErrorPath, pPrecision);

    return new ForgettingCompositeState(result, INITIAL_CONSTRAINTS);
  }
}
