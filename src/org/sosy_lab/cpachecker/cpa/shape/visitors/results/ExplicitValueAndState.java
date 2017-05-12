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
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.UnknownValue;

public class ExplicitValueAndState extends ShapeValueAndState {

  private ExplicitValueAndState(ShapeState pState, ShapeExplicitValue pValue) {
    super(pState, pValue);
  }

  public static ExplicitValueAndState of(ShapeState pState) {
    return new ExplicitValueAndState(pState, UnknownValue.getInstance());
  }

  public static ExplicitValueAndState of(ShapeState pState, ShapeExplicitValue pValue) {
    return new ExplicitValueAndState(pState, pValue);
  }

  @Override
  public ShapeExplicitValue getObject() {
    return (ShapeExplicitValue) super.getObject();
  }
}
