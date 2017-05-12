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
package org.sosy_lab.cpachecker.cpa.pointer2;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Map.Entry;


public enum PointerDomain implements AbstractDomain {

  INSTANCE;

  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2) throws CPAException {
    PointerState state1 = (PointerState) pState1;
    PointerState state2 = (PointerState) pState2;
    PointerState result = state2;
    for (Entry<MemoryLocation, LocationSet> pointsToEntry : state1.getPointsToMap().entrySet()) {
      result = result.addPointsToInformation(pointsToEntry.getKey(), pointsToEntry.getValue());
    }
    if (result.equals(state2)) {
      return state2;
    }
    if (result.equals(state1)) {
      return state1;
    }
    return result;
  }

  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    if (pState1 == pState2) {
      return true;
    }
    PointerState state1 = (PointerState) pState1;
    PointerState state2 = (PointerState) pState2;
    for (Entry<MemoryLocation, LocationSet> pointsToEntry : state1.getPointsToMap().entrySet()) {
      LocationSet rightSide = state2.getPointsToSet(pointsToEntry.getKey());
      if (!rightSide.containsAll(pointsToEntry.getValue())) {
        return false;
      }
    }
    return true;
  }

}
