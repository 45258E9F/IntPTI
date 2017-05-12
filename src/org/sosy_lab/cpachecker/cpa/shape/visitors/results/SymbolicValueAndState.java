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
package org.sosy_lab.cpachecker.cpa.shape.visitors.results;

import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.UnknownValue;

public class SymbolicValueAndState extends ShapeValueAndState {

  protected SymbolicValueAndState(ShapeState pState, ShapeSymbolicValue pValue) {
    super(pState, pValue);
  }

  public static SymbolicValueAndState of(ShapeState pState) {
    return new SymbolicValueAndState(pState, UnknownValue.getInstance());
  }

  public static SymbolicValueAndState of(ShapeState pState, ShapeSymbolicValue pValue) {
    return new SymbolicValueAndState(pState, pValue);
  }

  @Override
  public ShapeSymbolicValue getObject() {
    return (ShapeSymbolicValue) super.getObject();
  }
}
