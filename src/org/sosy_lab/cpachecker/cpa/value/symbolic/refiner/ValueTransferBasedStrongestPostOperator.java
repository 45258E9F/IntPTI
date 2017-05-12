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
package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsTransferRelation;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisStrongestPostOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

import java.util.Collection;
import java.util.Deque;

/**
 * Strongest post-operator based on symbolic value analysis.
 */
public class ValueTransferBasedStrongestPostOperator
    implements SymbolicStrongestPostOperator {

  private final ValueAnalysisTransferRelation valueTransfer;
  // used for abstraction
  private final ValueAnalysisStrongestPostOperator valueStrongestPost;
  private final ConstraintsTransferRelation constraintsTransfer;

  public ValueTransferBasedStrongestPostOperator(
      final Solver pSolver,
      final LogManager pLogger,
      final Configuration pConfig,
      final CFA pCfa,
      final ShutdownNotifier pShutdownNotifier
  ) throws InvalidConfigurationException {


    valueTransfer =
        new ValueAnalysisTransferRelation(pConfig, pLogger, pCfa);

    valueStrongestPost = new ValueAnalysisStrongestPostOperator(pLogger, pConfig, pCfa);

    constraintsTransfer =
        new ConstraintsTransferRelation(pSolver,
            pCfa.getMachineModel(),
            pLogger,
            pConfig,
            pShutdownNotifier);
  }

  @Override
  public Optional<ForgettingCompositeState> getStrongestPost(
      final ForgettingCompositeState pOrigin,
      final Precision pPrecision,
      final CFAEdge pOperation
  ) throws CPAException, InterruptedException {

    ValueAnalysisState oldValues = getValueStateOfCompositeState(pOrigin);
    ConstraintsState oldConstraints = getConstraintsStateOfCompositeState(pOrigin);


    assert oldValues != null && oldConstraints != null;

    final Collection<ValueAnalysisState> successors =
        valueTransfer.getAbstractSuccessorsForEdge(oldValues, Lists.<AbstractState>newArrayList(),
            pPrecision, pOperation);

    if (isContradiction(successors)) {
      return Optional.absent();

    } else {
      final ValueAnalysisState valuesSuccessor = Iterables.getOnlyElement(successors);

      Collection<? extends AbstractState> constraintsSuccessors =
          constraintsTransfer.getAbstractSuccessorsForEdge(
              oldConstraints, Lists.<AbstractState>newArrayList(),
              SingletonPrecision.getInstance(), pOperation);

      if (isContradiction(constraintsSuccessors)) {
        return Optional.absent();
      }

      final ConstraintsState constraintsSuccessor =
          (ConstraintsState) Iterables.get(constraintsSuccessors, 0);

      Optional<ConstraintsState> constraintsStrengthenResult =
          strengthenConstraintsState(constraintsSuccessor, valuesSuccessor, pOperation);

      if (!constraintsStrengthenResult.isPresent()) {
        return Optional.absent();

      } else {
        Optional<ValueAnalysisState> valueStrengthenResult =
            strengthenValueState(valuesSuccessor, constraintsSuccessor, pPrecision, pOperation);

        if (!valueStrengthenResult.isPresent()) {
          return Optional.absent();
        }

        return Optional.of(
            getNewCompositeState(valueStrengthenResult.get(), constraintsStrengthenResult.get()));
      }
    }
  }

  private ValueAnalysisState getValueStateOfCompositeState(final ForgettingCompositeState pState) {
    return pState.getValueState();
  }

  private ConstraintsState getConstraintsStateOfCompositeState(
      final ForgettingCompositeState pState
  ) {
    return pState.getConstraintsState();
  }

  @Override
  public ForgettingCompositeState handleFunctionCall(
      final ForgettingCompositeState pStateBeforeCall,
      final CFAEdge pEdge,
      final Deque<ForgettingCompositeState> pCallstack
  ) {
    pCallstack.addLast(pStateBeforeCall);
    return pStateBeforeCall;
  }

  @Override
  public ForgettingCompositeState handleFunctionReturn(
      final ForgettingCompositeState pNext,
      final CFAEdge pEdge,
      final Deque<ForgettingCompositeState> pCallstack
  ) {
    final ForgettingCompositeState callState = pCallstack.removeLast();

    // Do not forget any information about constraints.
    // In constraints, IdExpressions are already resolved to symbolic expression and as such
    // independent of scope.
    final ConstraintsState constraintsState = getConstraintsStateOfCompositeState(pNext);

    ValueAnalysisState currentValueState = getValueStateOfCompositeState(pNext);
    ValueAnalysisState callStateValueState = getValueStateOfCompositeState(callState);

    currentValueState = currentValueState.rebuildStateAfterFunctionCall(
        callStateValueState, (FunctionExitNode) pEdge.getPredecessor());

    return getNewCompositeState(currentValueState, constraintsState);
  }

  @Override
  public ForgettingCompositeState performAbstraction(
      final ForgettingCompositeState pNext,
      final CFANode pCurrNode,
      final ARGPath pErrorPath,
      final Precision pPrecision
  ) {
    ValueAnalysisState oldValueState = getValueStateOfCompositeState(pNext);

    assert pPrecision instanceof VariableTrackingPrecision;
    ValueAnalysisState newValueState =
        valueStrongestPost.performAbstraction(oldValueState, pCurrNode, pErrorPath, pPrecision);

    return getNewCompositeState(newValueState, pNext.getConstraintsState());
  }

  private Optional<ValueAnalysisState> strengthenValueState(
      final ValueAnalysisState pValues,
      final ConstraintsState pConstraints,
      final Precision pPrecision,
      final CFAEdge pOperation
  ) throws CPATransferException {

    Collection<? extends AbstractState> strengthenResult =
        valueTransfer.strengthen(pValues,
            ImmutableList.<AbstractState>of(pConstraints),
            pOperation,
            pPrecision);

    if (isContradiction(strengthenResult)) {
      return Optional.absent();

    } else {
      final AbstractState onlyState = Iterables.getOnlyElement(strengthenResult);

      return Optional.of((ValueAnalysisState) onlyState);
    }
  }


  private Optional<ConstraintsState> strengthenConstraintsState(
      final ConstraintsState pConstraintsState,
      final ValueAnalysisState pValueState,
      final CFAEdge pOperation
  ) throws CPATransferException, InterruptedException {

    Collection<? extends AbstractState> successors =
        constraintsTransfer.strengthen(pConstraintsState,
            ImmutableList.<AbstractState>of(pValueState),
            pOperation,
            SingletonPrecision.getInstance());

    if (successors == null) {
      // nothing changed
      return Optional.of(pConstraintsState);

    } else if (isContradiction(successors)) {
      return Optional.absent();

    } else {
      final AbstractState onlyState = Iterables.getOnlyElement(successors);

      return Optional.of((ConstraintsState) onlyState);
    }
  }

  private boolean isContradiction(final Collection<? extends AbstractState> pAbstractStates) {
    return pAbstractStates.isEmpty();
  }

  private ForgettingCompositeState getNewCompositeState(
      final ValueAnalysisState pNextValueState,
      final ConstraintsState pConstraints) {

    return new ForgettingCompositeState(pNextValueState, pConstraints);
  }
}
