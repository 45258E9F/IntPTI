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
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;

import java.util.Objects;

/**
 * This class represents a reference to an enum constant.
 */
public class JEnumConstantExpression extends AbstractExpression implements JExpression {

  // TODO Change the representation of the constantName from String to JIdExpression

  private final String constantName;

  public JEnumConstantExpression(
      FileLocation pFileLocation,
      JClassType pType,
      String pConstantName) {
    super(pFileLocation, pType);

    constantName = pConstantName;
  }

  @Override
  public JClassType getExpressionType() {
    return (JClassType) super.getExpressionType();
  }

  public String getConstantName() {
    return constantName;
  }

  @Override
  public <R, X extends Exception> R accept(JRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString() {
    return constantName;
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
    result = prime * result + Objects.hashCode(constantName);
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

    if (!(obj instanceof JEnumConstantExpression)
        || !super.equals(obj)) {
      return false;
    }

    JEnumConstantExpression other = (JEnumConstantExpression) obj;

    return Objects.equals(other.constantName, constantName);
  }


}