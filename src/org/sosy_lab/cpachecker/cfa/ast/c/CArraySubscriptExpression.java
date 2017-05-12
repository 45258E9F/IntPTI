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

import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public final class CArraySubscriptExpression extends AArraySubscriptExpression
    implements CLeftHandSide {


  public CArraySubscriptExpression(
      final FileLocation pFileLocation,
      final CType pType,
      final CExpression pArrayExpression,
      final CExpression pSubscriptExpression) {
    super(pFileLocation, pType, pArrayExpression, pSubscriptExpression);
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
  }

  @Override
  public CExpression getArrayExpression() {
    return (CExpression) super.getArrayExpression();
  }

  @Override
  public CExpression getSubscriptExpression() {
    return (CExpression) super.getSubscriptExpression();
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
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    return result * prime + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CArraySubscriptExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}
