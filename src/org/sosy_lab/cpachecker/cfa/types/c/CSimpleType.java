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

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

public final class CSimpleType implements CType, Serializable {


  private static final long serialVersionUID = -8279630814725098867L;
  private final CBasicType type;
  private final boolean isLong;
  private final boolean isShort;
  private final boolean isSigned;
  private final boolean isUnsigned;
  private final boolean isComplex;
  private final boolean isImaginary;
  private final boolean isLongLong;
  private boolean isConst;
  private boolean isVolatile;

  public CSimpleType(
      final boolean pConst, final boolean pVolatile,
      final CBasicType pType, final boolean pIsLong, final boolean pIsShort,
      final boolean pIsSigned, final boolean pIsUnsigned,
      final boolean pIsComplex, final boolean pIsImaginary,
      final boolean pIsLongLong) {

    // FIX: for some reasons (probably the issue of CDT), types such as 'short' and 'long' cannot
    // be recognized correctly as integer type.
    checkNotNull(pType);
    if (pType == CBasicType.UNSPECIFIED && (pIsLong || pIsShort || pIsSigned || pIsUnsigned ||
        pIsLongLong)) {
      type = CBasicType.INT;
    } else {
      type = pType;
    }

    isConst = pConst;
    isVolatile = pVolatile;
    isLong = pIsLong;
    isShort = pIsShort;
    isSigned = pIsSigned;
    isUnsigned = pIsUnsigned;
    isComplex = pIsComplex;
    isImaginary = pIsImaginary;
    isLongLong = pIsLongLong;
  }

  @Override
  public boolean isConst() {
    return isConst;
  }

  @Override
  public boolean isVolatile() {
    return isVolatile;
  }

  public CBasicType getType() {
    return type;
  }

  public boolean isLong() {
    return isLong;
  }

  public boolean isShort() {
    return isShort;
  }

  public boolean isSigned() {
    return isSigned;
  }

  public boolean isGeneralSigned() {
    return (type == CBasicType.INT && !isSigned && !isUnsigned) || isSigned;
  }

  public boolean isUnsigned() {
    return isUnsigned;
  }

  public boolean isComplex() {
    return isComplex;
  }

  public boolean isImaginary() {
    return isImaginary;
  }

  public boolean isLongLong() {
    return isLongLong;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(isComplex);
    result = prime * result + Objects.hashCode(isConst);
    result = prime * result + Objects.hashCode(isVolatile);
    result = prime * result + Objects.hashCode(isImaginary);
    result = prime * result + Objects.hashCode(isLong);
    result = prime * result + Objects.hashCode(isLongLong);
    result = prime * result + Objects.hashCode(isShort);
    result = prime * result + Objects.hashCode(isSigned);
    result = prime * result + Objects.hashCode(isUnsigned);
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

    if (!(obj instanceof CSimpleType)) {
      return false;
    }

    CSimpleType other = (CSimpleType) obj;

    // FIX: for int type, signed property is implicit
    boolean thisSigned = (type == CBasicType.INT && !isSigned && !isUnsigned) || isSigned;
    boolean thatSigned = (other.type == CBasicType.INT && !other.isSigned && !other.isUnsigned)
        || other.isSigned;

    return isComplex == other.isComplex && isConst == other.isConst
        && isVolatile == other.isVolatile && isImaginary == other.isImaginary
        && isLong == other.isLong && isLongLong == other.isLongLong
        && isShort == other.isShort && thisSigned == thatSigned
        && isUnsigned == other.isUnsigned && type == other.type;
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public String toString() {
    return toASTString("");
  }

  @Override
  public String toASTString(String pDeclarator) {
    checkNotNull(pDeclarator);
    List<String> parts = new ArrayList<>();

    if (isConst()) {
      parts.add("const");
    }
    if (isVolatile()) {
      parts.add("volatile");
    }

    if (isUnsigned) {
      parts.add("unsigned");
    } else if (isSigned) {
      parts.add("signed");
    }

    if (isLongLong) {
      parts.add("long long");
    } else if (isLong) {
      parts.add("long");
    } else if (isShort) {
      parts.add("short");
    }

    if (isImaginary) {
      parts.add("_Imaginary");
    }
    if (isComplex) {
      parts.add("_Complex");
    }

    parts.add(Strings.emptyToNull(type.toASTString()));
    parts.add(Strings.emptyToNull(pDeclarator));

    return Joiner.on(' ').skipNulls().join(parts);
  }

  @Override
  public CSimpleType getCanonicalType() {
    return getCanonicalType(false, false);
  }

  @Override
  public CSimpleType getCanonicalType(boolean pForceConst, boolean pForceVolatile) {
    CBasicType newType = type;
    if (newType == CBasicType.UNSPECIFIED) {
      newType = CBasicType.INT;
    }

    boolean newIsSigned = isSigned;
    if (newType == CBasicType.INT && !isSigned && !isUnsigned) {
      newIsSigned = true;
    }
    return new CSimpleType(isConst || pForceConst, isVolatile || pForceVolatile, newType, isLong,
        isShort, newIsSigned, isUnsigned, isComplex, isImaginary, isLongLong);
  }
}
