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


import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

import java.util.Objects;

/**
 * This class represents the boolean literal AST node type.
 *
 * BooleanLiteral:
 * true
 * false
 */
public class JBooleanLiteralExpression extends ALiteralExpression implements JLiteralExpression {


  final Boolean value;

  public JBooleanLiteralExpression(FileLocation pFileLocation, boolean pValue) {
    super(pFileLocation, JSimpleType.getBoolean());
    value = pValue;
  }

  @Override
  public Boolean getValue() {
    return value;
  }

  @Override
  public String toASTString() {
    if (value) {
      return "true";
    } else {
      return "false";
    }
  }

  @Override
  public JType getExpressionType() {
    return (JType) super.getExpressionType();
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
    int result = super.hashCode();
    result = prime * result + Objects.hashCode(value);
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

    if (!(obj instanceof JBooleanLiteralExpression)
        || !super.equals(obj)) {
      return false;
    }

    JBooleanLiteralExpression other = (JBooleanLiteralExpression) obj;

    return Objects.equals(other.value, value);
  }

}
