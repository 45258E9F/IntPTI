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
package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import java.util.Objects;

public final class CTypeIdExpression extends AbstractExpression implements CExpression {

  private final TypeIdOperator operator;
  private final CType type;

  public CTypeIdExpression(
      final FileLocation pFileLocation,
      final CType pExpressionType, final TypeIdOperator pOperator,
      final CType pType) {
    super(pFileLocation, pExpressionType);
    operator = pOperator;
    type = pType;
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
  }

  public TypeIdOperator getOperator() {
    return operator;
  }

  public CType getType() {
    return type;
  }

  @Override
  public <R, X extends Exception> R accept(CExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  public enum TypeIdOperator {
    SIZEOF,
    TYPEID,
    ALIGNOF,
    TYPEOF,;

    /**
     * Returns the string representation of this operator
     */
    public String getOperator() {
      return toString().toLowerCase();
    }
  }

  @Override
  public String toASTString() {
    return operator.getOperator() + "(" + type.toASTString("") + ")";
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(operator);
    result = prime * result + Objects.hashCode(type);
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

    if (!(obj instanceof CTypeIdExpression)
        || !super.equals(obj)) {
      return false;
    }

    CTypeIdExpression other = (CTypeIdExpression) obj;

    return Objects.equals(other.operator, operator)
        && Objects.equals(other.type, type);
  }

}
