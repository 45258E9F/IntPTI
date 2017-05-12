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

import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/**
 * This class represents the method invocation statement AST node type.
 *
 * This kind of node is used to convert an method invocation
 * into a statement by wrapping it. Note that expressions {@link JExpression}
 * in this AST are all side effect free. That is the reason we don't use
 * {@link JExpressionStatement} for method invocations.
 */
public class JMethodInvocationStatement extends AFunctionCallStatement
    implements JStatement, JMethodOrConstructorInvocation {

  public JMethodInvocationStatement(
      FileLocation pFileLocation,
      JMethodInvocationExpression pFunctionCall) {
    super(pFileLocation, pFunctionCall);
  }

  @Override
  public JMethodInvocationExpression getFunctionCallExpression() {
    return (JMethodInvocationExpression) super.getFunctionCallExpression();
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

    if (!(obj instanceof JMethodInvocationStatement)) {
      return false;
    }

    return super.equals(obj);
  }
}