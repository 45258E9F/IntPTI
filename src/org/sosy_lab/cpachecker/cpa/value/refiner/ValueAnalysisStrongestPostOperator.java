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
package org.sosy_lab.cpachecker.cpa.value.refiner;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssignmentsInPathCondition.UniqueAssignmentsInPathConditionState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Set;

/**
 * Strongest post-operator using {@link ValueAnalysisTransferRelation}.
 */
public class ValueAnalysisStrongestPostOperator
    implements StrongestPostOperator<ValueAnalysisState> {

  private final ValueAnalysisTransferRelation transfer;

  public ValueAnalysisStrongestPostOperator(
      final LogManager pLogger,
      final Configuration pConfig,
      final CFA pCfa
  ) throws InvalidConfigurationException {

    transfer = new ValueAnalysisTransferRelation(pConfig, pLogger, pCfa);
  }

  @Override
  public Optional<ValueAnalysisState> getStrongestPost(
      final ValueAnalysisState pOrigin,
      final Precision pPrecision,
      final CFAEdge pOperation
  ) throws CPAException {

    final Collection<ValueAnalysisState> successors =
        transfer.getAbstractSuccessorsForEdge(pOrigin, Lists.<AbstractState>newArrayList(),
            pPrecision, pOperation);

    if (successors.isEmpty()) {
      return Optional.absent();

    } else {
      return Optional.of(Iterables.getOnlyElement(successors));
    }
  }

  @Override
  public ValueAnalysisState handleFunctionCall(
      ValueAnalysisState state, CFAEdge edge,
      Deque<ValueAnalysisState> callstack) {
    callstack.addLast(state);
    return state;
  }

  @Override
  public ValueAnalysisState handleFunctionReturn(
      ValueAnalysisState next, CFAEdge edge,
      Deque<ValueAnalysisState> callstack) {

    final ValueAnalysisState callState = callstack.removeLast();
    return next.rebuildStateAfterFunctionCall(callState, (FunctionExitNode) edge.getPredecessor());
  }

  @Override
  public ValueAnalysisState performAbstraction(
      final ValueAnalysisState pNext,
      final CFANode pCurrNode,
      final ARGPath pErrorPath,
      final Precision pPrecision
  ) {

    assert pPrecision instanceof VariableTrackingPrecision;

    VariableTrackingPrecision precision = (VariableTrackingPrecision) pPrecision;

    final boolean performAbstraction = precision.allowsAbstraction();
    final Collection<MemoryLocation> exceedingMemoryLocations =
        obtainExceedingMemoryLocations(pErrorPath);

    if (performAbstraction) {
      for (MemoryLocation memoryLocation : pNext.getTrackedMemoryLocations()) {
        if (!precision.isTracking(memoryLocation,
            pNext.getTypeForMemoryLocation(memoryLocation),
            pCurrNode)) {
          pNext.forget(memoryLocation);
        }
      }
    }

    for (MemoryLocation exceedingMemoryLocation : exceedingMemoryLocations) {
      pNext.forget(exceedingMemoryLocation);
    }

    return pNext;
  }

  protected Set<MemoryLocation> obtainExceedingMemoryLocations(final ARGPath pPath) {
    UniqueAssignmentsInPathConditionState assignments =
        AbstractStates.extractStateByType(pPath.getLastState(),
            UniqueAssignmentsInPathConditionState.class);

    if (assignments == null) {
      return Collections.emptySet();
    }

    return assignments.getMemoryLocationsExceedingThreshold();
  }
}
