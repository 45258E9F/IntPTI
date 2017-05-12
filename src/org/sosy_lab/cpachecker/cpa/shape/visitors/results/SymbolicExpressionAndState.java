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
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicExpression;

public class SymbolicExpressionAndState extends ShapeObjectAndState<SymbolicExpression> {

  private SymbolicExpressionAndState(ShapeState pState, SymbolicExpression pSe) {
    super(pState, pSe);
  }

  public static SymbolicExpressionAndState of(ShapeState pState, SymbolicExpression pSe) {
    return new SymbolicExpressionAndState(pState, pSe);
  }

  @Override
  public SymbolicExpression getObject() {
    return super.getObject();
  }
}
