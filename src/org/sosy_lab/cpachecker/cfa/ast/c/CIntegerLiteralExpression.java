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

import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import java.math.BigInteger;

public class CIntegerLiteralExpression extends AIntegerLiteralExpression
    implements CLiteralExpression {

  public static final CIntegerLiteralExpression ONE = createDummyLiteral(1L, CNumericTypes.INT);
  public static final CIntegerLiteralExpression ZERO = createDummyLiteral(0L, CNumericTypes.INT);

  public static CIntegerLiteralExpression createDummyLiteral(long value, CType type) {
    return new CIntegerLiteralExpression(FileLocation.DUMMY, type, BigInteger.valueOf(value));
  }

  public CIntegerLiteralExpression(
      FileLocation pFileLocation,
      CType pType,
      BigInteger pValue) {
    super(pFileLocation, pType, pValue);
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
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

  @Override
  public String toASTString() {
    String suffix = "";

    CType cType = getExpressionType();
    if (cType instanceof CSimpleType) {
      CSimpleType type = (CSimpleType) cType;
      if (type.isUnsigned()) {
        suffix += "U";
      }
      if (type.isLong()) {
        suffix += "L";
      } else if (type.isLongLong()) {
        suffix += "LL";
      }
    }

    return getValue().toString() + suffix;
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

    if (!(obj instanceof CIntegerLiteralExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}
