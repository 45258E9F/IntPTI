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
package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.types.java.JType;


/**
 * Interface of Side effect free Expressions.
 */
public interface JExpression extends JRightHandSide, AExpression {

  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X;

  @Override
  public JType getExpressionType();

}
