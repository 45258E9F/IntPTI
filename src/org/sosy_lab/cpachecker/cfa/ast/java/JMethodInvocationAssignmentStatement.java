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

import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/**
 * This class represents an assignment with a method invocation as right hand side.
 * In the cfa, every method invocation in an expression is transformed to have its
 * own JMethodInvocationAssignmentStatement and a temporary variable to simplify analysis.
 */
public class JMethodInvocationAssignmentStatement extends AFunctionCallAssignmentStatement
    implements JAssignment, JStatement, JMethodOrConstructorInvocation {

  public JMethodInvocationAssignmentStatement(
      FileLocation pFileLocation, JLeftHandSide pLeftHandSide,
      JMethodInvocationExpression pRightHandSide) {
    super(pFileLocation, pLeftHandSide, pRightHandSide);

  }

  @Override
  public JMethodInvocationExpression getFunctionCallExpression() {
    return (JMethodInvocationExpression) super.getFunctionCallExpression();
  }

  @Override
  public JLeftHandSide getLeftHandSide() {
    return (JLeftHandSide) super.getLeftHandSide();
  }

  @Override
  public JMethodInvocationExpression getRightHandSide() {
    return (JMethodInvocationExpression) super.getRightHandSide();
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

    if (!(obj instanceof JMethodInvocationAssignmentStatement)) {
      return false;
    }

    return super.equals(obj);
  }
}
