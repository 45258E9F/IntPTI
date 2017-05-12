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

import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;

import java.util.List;
import java.util.Objects;

/**
 * This class represents a Array initializer AST node type.
 *
 * ArrayInitializer:
 * { [ Expression { , Expression} [ , ]] }
 *
 * The List of initializerExpressions gives the expression
 * the array cell is initialized with from left to right.
 */
public class JArrayInitializer extends AbstractExpression
    implements JAstNode, JInitializer, JExpression {

  private final List<JExpression> initializerExpressions;

  public JArrayInitializer(
      FileLocation pFileLocation,
      List<JExpression> pInitializerExpression,
      JArrayType pType) {
    super(pFileLocation, pType);

    initializerExpressions = pInitializerExpression;
  }

  @Override
  public JArrayType getExpressionType() {
    return (JArrayType) super.getExpressionType();
  }

  public List<JExpression> getInitializerExpressions() {
    return initializerExpressions;
  }

  @Override
  public String toASTString() {

    StringBuilder astString = new StringBuilder("{");

    for (JExpression exp : initializerExpressions) {
      astString.append(exp.toASTString() + ", ");
    }

    if (!initializerExpressions.isEmpty()) {
      // delete ', ' at the end of the current string
      int stringLength = astString.length();

      astString.delete(stringLength - 2, stringLength);
    }

    astString.append("}");

    return astString.toString();
  }

  @Override
  public <R, X extends Exception> R accept(JRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(initializerExpressions);
    result = prime * result + super.hashCode();
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JArrayInitializer)
        || super.equals(obj)) {
      return false;
    }

    JArrayInitializer other = (JArrayInitializer) obj;

    return Objects.equals(other.initializerExpressions, initializerExpressions);
  }

}
