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

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.ArrayList;
import java.util.List;


public class CompositeReducer implements Reducer {

  private final List<Reducer> wrappedReducers;

  public CompositeReducer(List<Reducer> pWrappedReducers) {
    wrappedReducers = pWrappedReducers;
  }

  @Override
  public AbstractState getVariableReducedState(
      AbstractState pExpandedState, Block pContext,
      CFANode pLocation) {

    List<AbstractState> result = new ArrayList<>();
    int i = 0;
    for (AbstractState expandedState : ((CompositeState) pExpandedState).getWrappedStates()) {
      result.add(
          wrappedReducers.get(i++).getVariableReducedState(expandedState, pContext, pLocation));
    }
    return new CompositeState(result);
  }

  @Override
  public AbstractState getVariableExpandedState(
      AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) {

    List<AbstractState> rootStates = ((CompositeState) pRootState).getWrappedStates();
    List<AbstractState> reducedStates = ((CompositeState) pReducedState).getWrappedStates();

    List<AbstractState> result = new ArrayList<>();
    int i = 0;
    for (Pair<AbstractState, AbstractState> p : Pair.zipList(rootStates, reducedStates)) {
      result.add(wrappedReducers.get(i++)
          .getVariableExpandedState(p.getFirst(), pReducedContext, p.getSecond()));
    }
    return new CompositeState(result);
  }

  @Override
  public Object getHashCodeForState(AbstractState pElementKey, Precision pPrecisionKey) {

    List<AbstractState> elements = ((CompositeState) pElementKey).getWrappedStates();
    List<Precision> precisions = ((CompositePrecision) pPrecisionKey).getPrecisions();

    List<Object> result = new ArrayList<>(elements.size());
    int i = 0;
    for (Pair<AbstractState, Precision> p : Pair.zipList(elements, precisions)) {
      result.add(wrappedReducers.get(i++).getHashCodeForState(p.getFirst(), p.getSecond()));
    }
    return result;
  }

  @Override
  public Precision getVariableReducedPrecision(
      Precision pPrecision,
      Block pContext) {
    List<Precision> precisions = ((CompositePrecision) pPrecision).getPrecisions();
    List<Precision> result = new ArrayList<>(precisions.size());

    int i = 0;
    for (Precision precision : precisions) {
      result.add(wrappedReducers.get(i++).getVariableReducedPrecision(precision, pContext));
    }

    return new CompositePrecision(result);
  }

  @Override
  public Precision getVariableExpandedPrecision(
      Precision pRootPrecision,
      Block pRootContext,
      Precision pReducedPrecision) {
    List<Precision> rootPrecisions = ((CompositePrecision) pRootPrecision).getPrecisions();
    List<Precision> reducedPrecisions = ((CompositePrecision) pReducedPrecision).getPrecisions();
    List<Precision> result = new ArrayList<>(rootPrecisions.size());

    int i = 0;
    for (Precision rootPrecision : rootPrecisions) {
      result.add(wrappedReducers.get(i)
          .getVariableExpandedPrecision(rootPrecision, pRootContext, reducedPrecisions.get(i)));
      i++;
    }

    return new CompositePrecision(result);
  }

  @Override
  public int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision) {
    List<Precision> precisions = ((CompositePrecision) pPrecision).getPrecisions();
    List<Precision> otherPrecisions = ((CompositePrecision) pOtherPrecision).getPrecisions();

    int i = 0;
    int sum = 0;
    for (Precision rootPrecision : precisions) {
      sum +=
          wrappedReducers.get(i).measurePrecisionDifference(rootPrecision, otherPrecisions.get(i));
      i++;
    }

    return sum;
  }

  @Override
  public AbstractState getVariableReducedStateForProofChecking(
      AbstractState pExpandedState, Block pContext,
      CFANode pCallNode) {
    List<AbstractState> result = new ArrayList<>();
    int i = 0;
    for (AbstractState expandedState : ((CompositeState) pExpandedState).getWrappedStates()) {
      result.add(wrappedReducers.get(i++)
          .getVariableReducedStateForProofChecking(expandedState, pContext, pCallNode));
    }
    return new CompositeState(result);
  }

  @Override
  public AbstractState getVariableExpandedStateForProofChecking(
      AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) {
    List<AbstractState> rootStates = ((CompositeState) pRootState).getWrappedStates();
    List<AbstractState> reducedStates = ((CompositeState) pReducedState).getWrappedStates();

    List<AbstractState> result = new ArrayList<>();
    int i = 0;
    for (Pair<AbstractState, AbstractState> p : Pair.zipList(rootStates, reducedStates)) {
      result.add(wrappedReducers.get(i++)
          .getVariableExpandedStateForProofChecking(p.getFirst(), pReducedContext, p.getSecond()));
    }
    return new CompositeState(result);
  }

  @Override
  public AbstractState rebuildStateAfterFunctionCall(
      AbstractState pRootState, AbstractState pEntryState,
      AbstractState pExpandedState, FunctionExitNode exitLocation) {
    List<AbstractState> rootStates = ((CompositeState) pRootState).getWrappedStates();
    List<AbstractState> entryStates = ((CompositeState) pEntryState).getWrappedStates();
    List<AbstractState> expandedStates = ((CompositeState) pExpandedState).getWrappedStates();

    List<AbstractState> results = new ArrayList<>();
    for (int i = 0; i < rootStates.size(); i++) {
      results.add(wrappedReducers.get(i).rebuildStateAfterFunctionCall(
          rootStates.get(i), entryStates.get(i), expandedStates.get(i), exitLocation));
    }
    return new CompositeState(results);
  }
}
