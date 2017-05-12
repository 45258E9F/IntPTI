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
package org.sosy_lab.cpachecker.cpa.range.summary;

import org.sosy_lab.cpachecker.cpa.range.RangeState;
import org.sosy_lab.cpachecker.cpa.range.util.AbstractStateStore;

public class RangeStateStore extends AbstractStateStore<RangeState> {

  public RangeStateStore(RangeState pState) {
    super(pState);
  }

  @Override
  public void updateState(RangeState newState) {
    if (historyStates.size() < 1) {
      // insufficient to perform widening on states
      historyStates.push(currentState);
      currentState = newState;
    } else {
      currentState = currentState.widening(newState);
    }
  }

  @Override
  public boolean initializeState(RangeState newState) {
    if (historyStates.isEmpty()) {
      // no history states, we simply update state at the entry
      if (newState.isLessOrEqual(currentState)) {
        return false;
      }
      currentState = currentState.join(newState);
      return true;
    } else {
      // the bottom of stack should be the "state at the entry"
      RangeState zeroState = historyStates.get(0);
      if (newState.isLessOrEqual(zeroState)) {
        return false;
      }
      currentState = zeroState.join(newState);
      historyStates.clear();
      return true;
    }
  }

  @Override
  public RangeState getTotalState() {
    if (historyStates.isEmpty()) {
      return currentState;
    } else {
      RangeState zeroState = historyStates.get(0);
      return zeroState.join(currentState);
    }
  }
}
