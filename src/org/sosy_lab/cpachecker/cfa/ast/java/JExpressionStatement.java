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

import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/**
 * This class represents the expression statement AST node type.
 *
 * This kind of node is used to convert an expression
 * (Expression) into a statement (Statement) by wrapping it.
 *
 * ExpressionStatement:
 * StatementExpression ;
 *
 * Note that this class is only used for side effect free expressions.
 * For assignments with side effect free right hand sides,
 * we use {@link JExpressionAssignmentStatement}.
 * For method invocations we use {@link JMethodInvocationStatement}
 * And for method assignments, we use {@link JMethodInvocationAssignmentStatement}.
 */
public class JExpressionStatement extends AExpressionStatement implements JStatement {

  public JExpressionStatement(FileLocation pFileLocation, JExpression pExpression) {
    super(pFileLocation, pExpression);
  }

  @Override
  public JExpression getExpression() {
    return (JExpression) super.getExpression();
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

    if (!(obj instanceof JExpressionStatement)) {
      return false;
    }

    return super.equals(obj);
  }
}
