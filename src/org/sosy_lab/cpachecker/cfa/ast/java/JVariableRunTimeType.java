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
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

import java.util.Objects;


public class JVariableRunTimeType extends AbstractExpression implements JRunTimeTypeExpression {

  private final JIdExpression referencedVariable;

  public JVariableRunTimeType(FileLocation pFileLocation, JIdExpression pReferencedVariable) {
    super(pFileLocation, pReferencedVariable.getExpressionType());

    assert pReferencedVariable.getExpressionType() instanceof JClassOrInterfaceType
        || pReferencedVariable.getExpressionType() instanceof JArrayType;

    referencedVariable = pReferencedVariable;
    assert getReferencedVariable() != null;
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

  @Override
  public String toASTString() {
    return getReferencedVariable().getName() + "_getClass()";
  }

  public JIdExpression getReferencedVariable() {
    return referencedVariable;
  }

  @Override
  public boolean isThisReference() {
    return false;
  }

  @Override
  public boolean isVariableReference() {
    return true;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(referencedVariable);
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

    if (!(obj instanceof JVariableRunTimeType)
        || !super.equals(obj)) {
      return false;
    }

    JVariableRunTimeType other = (JVariableRunTimeType) obj;

    return Objects.equals(other.referencedVariable, referencedVariable);
  }

}
