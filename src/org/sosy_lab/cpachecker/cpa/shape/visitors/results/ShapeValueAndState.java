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
package org.sosy_lab.cpachecker.cpa.shape.visitors.results;

import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeValue;
import org.sosy_lab.cpachecker.cpa.shape.values.UnknownValue;

public class ShapeValueAndState extends ShapeObjectAndState<ShapeValue> {

  protected ShapeValueAndState(ShapeState pState, ShapeValue pValue) {
    super(pState, pValue);
  }

  public static ShapeValueAndState of(ShapeState pState) {
    return new ShapeValueAndState(pState, UnknownValue.getInstance());
  }

  public static ShapeValueAndState of(ShapeState pState, ShapeValue pValue) {
    return new ShapeValueAndState(pState, pValue);
  }

}
