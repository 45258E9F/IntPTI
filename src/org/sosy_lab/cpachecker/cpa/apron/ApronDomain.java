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
package org.sosy_lab.cpachecker.cpa.apron;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import apron.Abstract0;
import apron.ApronException;

class ApronDomain implements AbstractDomain {

  private final LogManager logger;

  public ApronDomain(LogManager log) {
    logger = log;
  }

  @Override
  public boolean isLessOrEqual(AbstractState element1, AbstractState element2) {

    Map<ApronState, Set<ApronState>> covers = new HashMap<>();

    ApronState apronState1 = (ApronState) element1;
    ApronState apronState2 = (ApronState) element2;

    if (apronState2.getApronNativeState().isTop(apronState2.getManager().getManager())) {
      return true;
    }

    if (covers.containsKey(apronState2) && ((HashSet<ApronState>) (covers.get(apronState2)))
        .contains(apronState1)) {
      return true;
    }

    try {
      return apronState1.isLessOrEquals(apronState2);
    } catch (ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }
  }

  @Override
  public AbstractState join(AbstractState successor, AbstractState reached) {
    Pair<ApronState, ApronState> shrinkedStates;
    Abstract0 newApronState;
    ApronState firstState;
    try {
      shrinkedStates = getShrinkedStates((ApronState) successor, (ApronState) reached);
      firstState = shrinkedStates.getFirst();
      newApronState = firstState.getApronNativeState()
          .joinCopy(firstState.getManager().getManager(),
              shrinkedStates.getSecond().getApronNativeState());

    } catch (ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }

    ApronState newState = new ApronState(newApronState,
        firstState.getManager(),
        shrinkedStates.getFirst().getIntegerVariableToIndexMap(),
        shrinkedStates.getFirst().getRealVariableToIndexMap(),
        shrinkedStates.getFirst().getVariableToTypeMap(),
        shrinkedStates.getFirst().getVariableToCTypeMap(),
        ((ApronState) successor).isLoopHead(),
        logger);
    if (newState.equals(reached)) {
      return reached;
    } else if (newState.equals(successor)) {
      return successor;
    } else {
      return newState;
    }
  }

  public AbstractState widening(ApronState successorState, ApronState reachedState) {
    Pair<ApronState, ApronState> shrinkedStates;
    Abstract0 newApronState;
    if (reachedState.getApronNativeState().isTop(reachedState.getManager().getManager())) {
      return reachedState;
    }
    try {
      shrinkedStates = getShrinkedStates(successorState, reachedState);
      successorState = shrinkedStates.getFirst();
      reachedState = shrinkedStates.getSecond();

      newApronState = reachedState.getApronNativeState()
          .widening(reachedState.getManager().getManager(), successorState.getApronNativeState());

    } catch (ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }

    ApronState newState = new ApronState(newApronState,
        reachedState.getManager(),
        successorState.getIntegerVariableToIndexMap(),
        successorState.getRealVariableToIndexMap(),
        successorState.getVariableToTypeMap(),
        successorState.getVariableToCTypeMap(),
        successorState.isLoopHead(),
        logger);
    if (newState.equals(reachedState)) {
      return reachedState;
    } else if (newState.equals(successorState)) {
      return successorState;
    } else {
      return newState;
    }
  }

  private Pair<ApronState, ApronState> getShrinkedStates(ApronState succ, ApronState reached)
      throws ApronException {
    if (succ.sizeOfVariables() > reached.sizeOfVariables()) {
      Pair<ApronState, ApronState> tmp = succ.shrinkToFittingSize(reached);
      succ = tmp.getFirst();
      reached = tmp.getSecond();
    } else {
      Pair<ApronState, ApronState> tmp = reached.shrinkToFittingSize(succ);
      succ = tmp.getSecond();
      reached = tmp.getFirst();
    }
    return Pair.of(succ, reached);
  }
}
