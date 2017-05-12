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
package org.sosy_lab.cpachecker.core.bugfix.instance.integer;

import com.google.common.base.Objects;

import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.util.access.AccessPath;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class IntegerTypeConstraint {

  public enum IntegerTypePredicate {
    EQUAL,
    // two types are equal in the sense of range
    COVER,
    // one type contains all possible values of another type
    COVER_DECLARE,    // cover relation for declaration
  }

  private IntegerTypePredicate predicate;

  // operands are qualified names
  private String op1;
  // op2 can be a type string, or an identifier
  // thus, we still use string to represent the second operand here
  private String op2;

  private boolean isSoft;

  private IntegerTypeConstraint(
      IntegerTypePredicate pPredicate,
      @Nonnull String pOp1,
      @Nonnull String pOp2,
      boolean pIsSoft) {
    predicate = pPredicate;
    op1 = pOp1;
    op2 = pOp2;
    isSoft = pIsSoft;
  }

  @Nullable
  public static IntegerTypeConstraint of(
      IntegerTypePredicate pPredicate,
      AccessPath pPath,
      CSimpleType pType,
      boolean pIsSoft) {
    if (pPath != null && pPath.isDeclarationPath()) {
      String name = pPath.getQualifiedName();
      String type = toTypeString(pType);
      if (type != null && !name.equals(CVariableDeclaration.DUMMY_NAME)) {
        // then we can create a new type constraint
        return new IntegerTypeConstraint(pPredicate, name, type, pIsSoft);
      }
    }
    return null;
  }

  @Nullable
  public static IntegerTypeConstraint of(
      IntegerTypePredicate pPredicate, String qualifiedName,
      CSimpleType pType, boolean pIsSoft) {
    String type = toTypeString(pType);
    if (type != null && !qualifiedName.equals(CVariableDeclaration.DUMMY_NAME)) {
      return new IntegerTypeConstraint(pPredicate, qualifiedName, type, pIsSoft);
    }
    return null;
  }

  public IntegerTypePredicate getPredicate() {
    return predicate;
  }

  String getName1() {
    return op1;
  }

  String getName2() {
    return op2;
  }

  boolean getSoftness() {
    return isSoft;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(predicate, op1, op2, isSoft);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !(obj instanceof IntegerTypeConstraint)) {
      return false;
    }
    IntegerTypeConstraint that = (IntegerTypeConstraint) obj;
    return predicate == that.predicate && Objects.equal(op1, that.op1) && Objects.equal(op2, that
        .op2) && isSoft == that.isSoft;
  }

  @Nullable
  public static CSimpleType fromTypeString(@Nonnull String typeStr) {
    switch (typeStr) {
      case "!CHAR!":
        return CNumericTypes.CHAR;
      case "!UCHAR!":
        return CNumericTypes.UNSIGNED_CHAR;
      case "!SCHAR!":
        return CNumericTypes.SIGNED_CHAR;
      case "!SHORT!":
        return CNumericTypes.SHORT_INT;
      case "!USHORT!":
        return CNumericTypes.UNSIGNED_SHORT_INT;
      case "!INT!":
        return CNumericTypes.INT;
      case "!UINT!":
        return CNumericTypes.UNSIGNED_INT;
      case "!LINT!":
        return CNumericTypes.LONG_INT;
      case "!ULINT!":
        return CNumericTypes.UNSIGNED_LONG_INT;
      case "!LLINT!":
        return CNumericTypes.LONG_LONG_INT;
      case "!ULLINT!":
        return CNumericTypes.UNSIGNED_LONG_LONG_INT;
      default:
        // possibly "!OVERLONG!"
        return null;
    }
  }

  @Nullable
  static String toTypeString(@Nonnull CSimpleType type) {
    if (type.equals(CNumericTypes.CHAR)) {
      return "!CHAR!";
    } else if (type.equals(CNumericTypes.UNSIGNED_CHAR)) {
      return "!UCHAR!";
    } else if (type.equals(CNumericTypes.SIGNED_CHAR)) {
      return "!SCHAR!";
    } else if (type.equals(CNumericTypes.SHORT_INT)) {
      return "!SHORT!";
    } else if (type.equals(CNumericTypes.UNSIGNED_SHORT_INT)) {
      return "!USHORT!";
    } else if (type.equals(CNumericTypes.INT) || type.equals(CNumericTypes.SIGNED_INT)) {
      return "!INT!";
    } else if (type.equals(CNumericTypes.UNSIGNED_INT)) {
      return "!UINT!";
    } else if (type.equals(CNumericTypes.LONG_INT)) {
      return "!LINT!";
    } else if (type.equals(CNumericTypes.UNSIGNED_LONG_INT)) {
      return "!ULINT!";
    } else if (type.equals(CNumericTypes.LONG_LONG_INT)) {
      return "!LLINT!";
    } else if (type.equals(CNumericTypes.UNSIGNED_LONG_LONG_INT)) {
      return "!ULLINT!";
    } else {
      return null;
    }
  }

  @Nullable
  public static String toMethodString(@Nonnull CSimpleType type) {
    CBasicType basicType = type.getType();
    switch (basicType) {
      case CHAR: {
        if (type.isSigned()) {
          return "schar";
        } else if (type.isUnsigned()) {
          return "uchar";
        } else {
          return "char";
        }
      }
      case INT: {
        boolean isSigned = type.isGeneralSigned();
        if (type.isShort()) {
          return isSigned ? "short" : "ushort";
        } else if (type.isLong()) {
          return isSigned ? "lint" : "ulint";
        } else if (type.isLongLong()) {
          return isSigned ? "llint" : "ullint";
        } else {
          return isSigned ? "int" : "uint";
        }
      }
      default:
        return null;
    }
  }

}
