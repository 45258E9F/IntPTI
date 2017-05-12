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

import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/**
 * This class represents the assignment expression AST node type.
 *
 *
 * Assignment:
 * Expression = Expression
 *
 * Note that the assignment operator is always '='. All assignment expressions
 * are transformed into an assignment with '=' and a {@link JBinaryExpression}.
 *
 * Note also, that the expressions have to be side-effect free.
 */
public class JExpressionAssignmentStatement extends AExpressionAssignmentStatement
    implements JAssignment, JStatement {

  public JExpressionAssignmentStatement(
      FileLocation pFileLocation, JLeftHandSide pLeftHandSide,
      JExpression pRightHandSide) {
    super(pFileLocation, pLeftHandSide, pRightHandSide);
  }

  @Override
  public JLeftHandSide getLeftHandSide() {
    return (JLeftHandSide) super.getLeftHandSide();
  }

  @Override
  public JExpression getRightHandSide() {
    return (JExpression) super.getRightHandSide();
  }

  @Override
  public <R, X extends Exception> R accept(JStatementVisitor<R, X> v) throws X {
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

    if (!(obj instanceof JExpressionAssignmentStatement)) {
      return false;
    }

    return super.equals(obj);
  }
}
