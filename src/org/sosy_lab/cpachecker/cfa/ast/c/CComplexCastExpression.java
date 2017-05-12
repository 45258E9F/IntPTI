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

public final class CComplexCastExpression extends AbstractExpression implements CLeftHandSide {

  private final CExpression operand;
  private final CType type;
  /**
   * When isReal is false this is a cast to get the imaginary Part of the complex number
   */
  private final boolean isReal;

  public CComplexCastExpression(
      final FileLocation pFileLocation,
      final CType pExpressionType,
      final CExpression pOperand,
      final CType pType,
      final boolean pIsRealCast) {
    super(pFileLocation, pExpressionType);

    isReal = pIsRealCast;
    operand = pOperand;
    type = pType;
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
  }

  public CExpression getOperand() {
    return operand;
  }

  public CType getType() {
    return type;
  }

  public boolean isImaginaryCast() {
    return !isReal;
  }

  public boolean isRealCast() {
    return isReal;
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
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
  public <R, X extends Exception> R accept(CLeftHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString() {
    if (isReal) {
      return "__real__ " + operand.toASTString();
    } else {
      return "__imag__ " + operand.toASTString();
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(operand);
    result = prime * result + Objects.hashCode(type);
    result = prime * result + Objects.hashCode(isReal);
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

    if (!(obj instanceof CComplexCastExpression)
        || !super.equals(obj)) {
      return false;
    }

    CComplexCastExpression other = (CComplexCastExpression) obj;

    return Objects.equals(other.operand, operand) && Objects.equals(other.type, type)
        && Objects.equals(other.isReal, isReal);
  }

}
