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
package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * This class represents the array access expression AST node type.
 *
 * ArrayAccess:
 * Expression [ Expression ]
 *
 * The array expression gives the identifier of the array.
 * The subscript Expression gives the index of the arraycell to be read.
 */
public class JArraySubscriptExpression extends AArraySubscriptExpression implements JLeftHandSide {

  public JArraySubscriptExpression(
      FileLocation pFileLocation, JType pType, JExpression pArrayExpression,
      JExpression pSubscriptExpression) {
    super(pFileLocation, pType, pArrayExpression, pSubscriptExpression);
  }

  @Override
  public JType getExpressionType() {
    return (JType) super.getExpressionType();
  }

  @Override
  public JExpression getArrayExpression() {
    return (JExpression) super.getArrayExpression();
  }

  @Override
  public JExpression getSubscriptExpression() {
    return (JExpression) super.getSubscriptExpression();
  }

  @Override
  public <R, X extends Exception> R accept(JRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(JLeftHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JArraySubscriptExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}
