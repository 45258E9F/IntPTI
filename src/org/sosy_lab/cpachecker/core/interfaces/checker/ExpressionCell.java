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

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import java.util.List;

public class ExpressionCell<T extends AbstractState, S> {

  private T state;
  private List<AbstractState> otherStates;
  private List<S> operands;
  private S result;

  public ExpressionCell(
      T pState, List<AbstractState> pOtherStates, List<S>
      pOperands, S pResult) {
    state = pState;
    otherStates = pOtherStates;
    operands = pOperands;
    result = pResult;
  }

  public T getState() {
    return state;
  }

  public List<AbstractState> getOtherStates() {
    return otherStates;
  }

  public int getNumOfOperands() {
    return operands.size();
  }

  public List<S> getOperands() {
    return operands;
  }

  public S getOperand(int index) {
    Preconditions.checkArgument(index >= 0 && index < operands.size());
    return operands.get(index);
  }

  public S getResult() {
    return result;
  }

}
