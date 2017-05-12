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
package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class CUnaryExpression extends AUnaryExpression implements CExpression {


  public CUnaryExpression(
      final FileLocation pFileLocation,
      final CType pType, final CExpression pOperand,
      final UnaryOperator pOperator) {
    super(pFileLocation, pType, pOperand, pOperator);

  }

  @Override
  public CExpression getOperand() {
    return (CExpression) super.getOperand();
  }

  @Override
  public UnaryOperator getOperator() {
    return (UnaryOperator) super.getOperator();
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
  }

  @Override
  public String toASTString() {
    if (getOperator() == UnaryOperator.SIZEOF) {
      return getOperator().getOperator() + "(" + getOperand().toASTString() + ")";
    } else {
      return getOperator().getOperator() + getOperand().toParenthesizedASTString();
    }
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

  public static enum UnaryOperator implements AUnaryExpression.AUnaryOperator {
    MINUS("-"),
    AMPER("&"),
    TILDE("~"),
    SIZEOF("sizeof"),
    ALIGNOF("__alignof__"),;

    private final String mOp;

    private UnaryOperator(String pOp) {
      mOp = pOp;
    }

    /**
     * Returns the string representation of this operator (e.g. "*", "+").
     */
    @Override
    public String getOperator() {
      return mOp;
    }
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

    if (!(obj instanceof CUnaryExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}