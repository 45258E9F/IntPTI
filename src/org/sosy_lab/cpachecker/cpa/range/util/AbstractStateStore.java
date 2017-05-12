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
package org.sosy_lab.cpachecker.cpa.range.util;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import java.util.Stack;

/**
 * A state store for analyzing loop structure.
 * It consists of (1) current state, (2) history states. The history state queue is used for
 * performing widening.
 */
public abstract class AbstractStateStore<R extends AbstractState> {

  // state in use now
  protected R currentState;

  // current state does not contained in the history state stack
  // Invariant: the state before any iterations of loop should be on the bottom of the stack
  protected Stack<R> historyStates;


  protected AbstractStateStore(R pState) {
    currentState = pState;
    historyStates = new Stack<>();
  }

  public R getState() {
    return currentState;
  }

  /**
   * The state store is updated when we are about to visit a loop node again.
   */
  public abstract void updateState(R newState);

  /**
   * The state store is initialized when we are about to enter one loop.
   *
   * @return false if the initialized state has already been covered.
   */
  public abstract boolean initializeState(R newState);

  /**
   * Collect states and merge them into a total state that can summarize all the historical states.
   */
  public abstract R getTotalState();

}
