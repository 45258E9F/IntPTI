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
package org.sosy_lab.cpachecker.cpa.shape.constraint;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.constraint.CRVisitor;
import org.sosy_lab.cpachecker.cpa.shape.visitors.constraint.SEVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/**
 * The leaf node of symbolic expression. The value of constant SE can be symbolic, explicit or
 * unknown.
 */
public class ConstantSE implements SymbolicExpression {

  private final ShapeValue value;
  private final CType type;
  private final CExpression origExp;

  private static final String PREFIX = "SYMBOLIC_";

  public ConstantSE(ShapeValue pValue, CType pType, CExpression pOrigExp) {
    value = pValue;
    type = pType;
    origExp = pOrigExp;
  }

  /* **************** */
  /* override methods */
  /* **************** */

  @Override
  public ShapeValue getValue() {
    return value;
  }

  @Override
  public SymbolicKind getValueKind() {
    if (value instanceof KnownSymbolicValue) {
      return SymbolicKind.SYMBOLIC;
    } else if (value instanceof KnownExplicitValue) {
      return SymbolicKind.EXPLICIT;
    } else {
      return SymbolicKind.UNKNOWN;
    }
  }

  @Override
  public CType getType() {
    return type;
  }

  @Override
  public CExpression getOriginalExpression() {
    return origExp;
  }

  @Override
  public CExpression getSymbolicExpression() {
    if (value.isUnknown()) {
      return null;
    }
    // FIX1: handle different kinds of values wisely
    // FIX2: handle special floating value wisely (e.g. NaN)
    if (value instanceof KnownExplicitValue) {
      if (isIntegerType(type)) {
        BigInteger intValue = ((KnownExplicitValue) value).getAsBigInteger();
        return new CIntegerLiteralExpression(origExp.getFileLocation(), type, intValue);
      } else {
        assert (type instanceof CSimpleType);
        double dblValue = value.getAsDouble();
        return toFloatLiteral(dblValue, (CSimpleType) type);
      }
    } else {
      Preconditions.checkArgument(value instanceof KnownSymbolicValue);
      String symbolicId = convertToSymbolicIdentifier();
      CVariableDeclaration declaration = getSymbolicDeclaration(symbolicId);
      return new CIdExpression(origExp.getFileLocation(), declaration);
    }
  }

  @Override
  public <T> T accept(SEVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public <T> T accept(CRVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof ConstantSE)) {
      return false;
    }
    ConstantSE other = (ConstantSE) obj;
    return value.equals(other.value) && type.getCanonicalType().equals(other.type
        .getCanonicalType());
  }

  @Override
  public String toString() {
    CExpression ce = getSymbolicExpression();
    return ce == null ? "NULL" : ce.toString();
  }

  /* ****************** */
  /* conversion methods */
  /* ****************** */

  private String convertToSymbolicIdentifier() {
    Long id = value.getAsLong();
    return PREFIX.concat(Long.toString(id));
  }

  private CVariableDeclaration getSymbolicDeclaration(String newID) {
    return new CVariableDeclaration(FileLocation.DUMMY, false, CStorageClass.AUTO, type, newID,
        newID, newID, null);
  }

  private boolean isIntegerType(CType pType) {
    CType canonicalType = pType.getCanonicalType();
    // sometimes, though very rare, we use literal to represent pointer value
    return canonicalType instanceof CPointerType || canonicalType instanceof CEnumType || (
        (CSimpleType) canonicalType).getType().isIntegerType();
  }

  private CExpression toFloatLiteral(double pValue, CSimpleType pType) {
    if (Double.isNaN(pValue) || Double.isInfinite(pValue)) {
      // we can represent a special floating point by constructing a division expression:
      // (1) +inf: 1 / 0
      // (2) -inf: -1 / 0
      // (3) NaN: 0 / 0
      CExpression dividend;
      if (Double.isNaN(pValue)) {
        dividend = new CIntegerLiteralExpression(FileLocation.DUMMY, pType, BigInteger.valueOf(0));
      } else if (pValue == Double.POSITIVE_INFINITY) {
        dividend = new CIntegerLiteralExpression(FileLocation.DUMMY, pType, BigInteger.valueOf(1));
      } else {
        dividend = new CIntegerLiteralExpression(FileLocation.DUMMY, pType, BigInteger.valueOf(-1));
      }
      return new CBinaryExpression(origExp.getFileLocation(), pType, pType, dividend,
          CIntegerLiteralExpression.ZERO, BinaryOperator.DIVIDE);
    } else {
      return new CFloatLiteralExpression(origExp.getFileLocation(), pType, BigDecimal.valueOf
          (pValue));
    }
  }

  /**
   * Check if the specified raw string of formula term corresponds to a symbolic value.
   *
   * @param id raw identifier
   * @return TRUE if raw identifier is derived from a symbolic value, and FALSE otherwise.
   */
  public static boolean isSymbolicTerm(String id) {
    if (id == null) {
      return false;
    }
    String varName = null;
    try {
      varName = FormulaManagerView.parseName(id).getFirst();
    } catch (IllegalArgumentException e) {
      return false;
    }

    if (varName == null) {
      return false;
    }
    return varName.matches(PREFIX + "[0-9]*");
  }

  /**
   * Convert a raw identifier to a symbolic value.
   * Precondition: the specified raw identifier should be a symbolic term (i.e. pass the
   * {@link #isSymbolicTerm(String)} test).
   *
   * @param id raw identifier
   * @return the corresponding symbolic value.
   */
  public static KnownSymbolicValue toSymbolicValue(String id) {
    Preconditions.checkNotNull(id);
    final String varName = FormulaManagerView.parseName(id).getFirst();
    Preconditions.checkNotNull(varName);
    final long number = Long.parseLong(varName.substring(PREFIX.length()));
    return KnownSymbolicValue.valueOf(number);
  }

  /**
   * @return TRUE if the constant symbolic expression is unknown, in which case we cannot make a
   * predicate using formula converter.
   */
  public boolean isUnknown() {
    return value.isUnknown();
  }

}
