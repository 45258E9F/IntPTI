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
package org.sosy_lab.cpachecker.cpa.pointer2;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Map.Entry;

public enum Pointer2Domain implements AbstractDomain {

  INSTANCE;

  @Override
  public AbstractState join(
      AbstractState state1, AbstractState state2) throws CPAException, InterruptedException {
    Pointer2State s1 = (Pointer2State) state1;
    Pointer2State s2 = (Pointer2State) state2;
    Pointer2State result = s2;
    for (Entry<MemoryLocation, LocationSet> pointsToEntry : s1.getPointsToMap().entrySet()) {
      result = result.mergePointsToInformation(pointsToEntry.getKey(), pointsToEntry.getValue());
    }
    if (result.equals(s2)) {
      // ATTENTION: we must return the exactly original object as result, otherwise the analysis
      // may not terminate
      return s2;
    }
    if (result.equals(s1)) {
      return s1;
    }
    return result;
  }

  @Override
  public boolean isLessOrEqual(
      AbstractState state1, AbstractState state2) throws CPAException, InterruptedException {
    if (state1 == state2) {
      return true;
    }
    Pointer2State s1 = (Pointer2State) state1;
    Pointer2State s2 = (Pointer2State) state2;
    // state1: successor
    // state2: reached
    // this method returns true if reached state contains all information in successor state
    for (Entry<MemoryLocation, LocationSet> pointsToEntry : s1.getPointsToMap().entrySet()) {
      LocationSet reachedTargets = s2.getPointsToSet(pointsToEntry.getKey());
      if (!reachedTargets.containsAll(pointsToEntry.getValue())) {
        return false;
      }
    }
    return true;
  }
}
