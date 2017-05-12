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
package org.sosy_lab.cpachecker.core.interfaces.checker;


import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import java.util.List;

public class AssignmentCell<T extends AbstractState, S> {

  /**
   * State before assignment
   */
  T state;

  List<AbstractState> otherStates;
  /**
   * The value of the right-hand-side expression before assignment
   */
  S value;


  public AssignmentCell(T pState, List<AbstractState> pOtherStates, S pValue) {
    state = pState;
    otherStates = pOtherStates;
    value = pValue;
  }

  public T getState() {
    return state;
  }

  public S getRightValue() {
    return value;
  }

  public List<AbstractState> getOtherStates() {
    return otherStates;
  }
}
