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
package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Objects;

import javax.annotation.Nullable;


public final class CPointerType implements CType, Serializable {

  private static final long serialVersionUID = -6423006826454509009L;
  public static final CPointerType POINTER_TO_VOID = new CPointerType(false, false, CVoidType.VOID);
  public static final CPointerType POINTER_TO_CONST_CHAR =
      new CPointerType(false, false, CNumericTypes.CHAR.getCanonicalType(true, false));

  private final CType type;
  private boolean isConst;
  private boolean isVolatile;

  public CPointerType(
      final boolean pConst, final boolean pVolatile,
      final CType pType) {
    isConst = pConst;
    isVolatile = pVolatile;
    type = checkNotNull(pType);
  }

  @Override
  public boolean isConst() {
    return isConst;
  }

  @Override
  public boolean isVolatile() {
    return isVolatile;
  }

  public CType getType() {
    return type;
  }

  @Override
  public String toString() {
    String decl;

    decl = "(" + type.toString() + ")*";


    return (isConst() ? "const " : "")
        + (isVolatile() ? "volatile " : "")
        + decl;
  }

  @Override
  public String toASTString(String pDeclarator) {
    checkNotNull(pDeclarator);
    // ugly hack but it works:
    // We need to insert the "*" between the type and the name (e.g. "int *var").
    String decl;

    if (type instanceof CArrayType) {
      decl = type.toASTString("(*" + pDeclarator + ")");
    } else {
      decl = type.toASTString("*" + pDeclarator);
    }

    return (isConst() ? "const " : "")
        + (isVolatile() ? "volatile " : "")
        + decl;
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(isConst);
    result = prime * result + Objects.hashCode(isVolatile);
    result = prime * result + Objects.hashCode(type);
    return result;
  }

  /**
   * Be careful, this method compares the CType as it is to the given object,
   * typedefs won't be resolved. If you want to compare the type without having
   * typedefs in it use #getCanonicalType().equals()
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }

    if (!(obj instanceof CPointerType)) {
      return false;
    }

    CPointerType other = (CPointerType) obj;

    return isConst == other.isConst && isVolatile == other.isVolatile
        && Objects.equals(type, other.type);
  }

  @Override
  public CPointerType getCanonicalType() {
    return getCanonicalType(false, false);
  }

  @Override
  public CPointerType getCanonicalType(boolean pForceConst, boolean pForceVolatile) {
    return new CPointerType(isConst || pForceConst, isVolatile || pForceVolatile,
        type.getCanonicalType());
  }
}
