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
package org.sosy_lab.cpachecker.cpa.bdd;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;

import java.util.HashSet;
import java.util.Set;

public class BDDReducer implements Reducer {

  private final PredicateManager predmgr;

  BDDReducer(PredicateManager pPredmgr) {
    predmgr = pPredmgr;
  }

  private Set<String> getVarsOfBlock(Block pBlock) {
    Set<String> vars = new HashSet<>();
    for (ReferencedVariable referencedVar : pBlock.getReferencedVariables()) {
      vars.add(referencedVar.getName());
    }
    return vars;
  }

  @Override
  public AbstractState getVariableReducedState(
      AbstractState pExpandedState,
      Block pBlock,
      CFANode pCallNode) {
    BDDState state = (BDDState) pExpandedState;

    final Set<String> trackedVars = predmgr.getTrackedVars().keySet();
    final Set<String> blockVars = getVarsOfBlock(pBlock);
    for (final String var : trackedVars) {
      if (!blockVars.contains(var)) {
        int size = predmgr.getTrackedVars().get(var);
        Region[] toRemove = predmgr.createPredicateWithoutPrecisionCheck(var, size);
        state = state.forget(toRemove);
      }
    }

    return state;
  }

  @Override
  public AbstractState getVariableExpandedState(
      AbstractState pRootState,
      Block reducedContext,
      AbstractState pReducedState) {
    BDDState state = (BDDState) pRootState;
    BDDState reducedState = (BDDState) pReducedState;

    // remove all vars, that are used in the block
    final Set<String> trackedVars = predmgr.getTrackedVars().keySet();
    final Set<String> blockVars = getVarsOfBlock(reducedContext);
    for (final String var : trackedVars) {
      if (blockVars.contains(var)) {
        int size = predmgr.getTrackedVars().get(var);
        Region[] toRemove = predmgr.createPredicateWithoutPrecisionCheck(var, size);
        state = state.forget(toRemove);
      }
    }

    // TODO maybe we have to add some heuristics like in BAMPredicateReducer,
    // because we existentially quantify "block-inner" variables from formulas like "outer==inner".
    // This is sound, but leads to weaker formulas, maybe to weak for a useful analysis.
    // Or simpler solution: We could replace this Reducer with a NoOpReducer.

    // add information from block to state
    state = state.addConstraint(reducedState.getRegion());

    return state;
  }

  @Override
  public Precision getVariableReducedPrecision(Precision precision, Block context) {
    // TODO what to do?
    return precision;
  }

  @Override
  public Precision getVariableExpandedPrecision(
      Precision rootPrecision,
      Block rootContext,
      Precision reducedPrecision) {
    // TODO what to do?
    return reducedPrecision;
  }

  @Override
  public Object getHashCodeForState(AbstractState stateKey, Precision precisionKey) {
    return Pair.of(((BDDState) stateKey).getRegion(), precisionKey);
  }

  @Override
  public int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision) {
    return 0;
  }

  @Override
  public AbstractState getVariableReducedStateForProofChecking(
      AbstractState pExpandedState,
      Block pContext,
      CFANode pCallNode) {
    // TODO what to do?
    return getVariableReducedState(pExpandedState, pContext, pCallNode);
  }

  @Override
  public AbstractState getVariableExpandedStateForProofChecking(
      AbstractState pRootState,
      Block pReducedContext,
      AbstractState pReducedState) {
    // TODO what to do?
    return getVariableExpandedState(pRootState, pReducedContext, pReducedState);
  }

  @Override
  public AbstractState rebuildStateAfterFunctionCall(
      AbstractState rootState, AbstractState entryState,
      AbstractState expandedState, FunctionExitNode exitLocation) {
    throw new UnsupportedOperationException("not implemented");
  }
}
