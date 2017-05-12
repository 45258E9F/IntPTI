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
package org.sosy_lab.cpachecker.cpa.shape.constraint;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeExplicitValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeValue;
import org.sosy_lab.cpachecker.cpa.shape.values.UnknownValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.CoreShapeAdapter;
import org.sosy_lab.cpachecker.cpa.shape.visitors.value.SymbolicExpressionVisitor;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

public final class SEs {

  public static boolean isUnknown(SymbolicExpression se) {
    return (se instanceof ConstantSE) && se.getValueKind() == SymbolicKind.UNKNOWN;
  }

  public static SymbolicExpression toUnknown(CType t) {
    return new ConstantSE(UnknownValue.getInstance(), t, CIdExpression.DUMMY_ID(t));
  }

  private static SymbolicExpression toUnknown(CExpression e) {
    return new ConstantSE(UnknownValue.getInstance(), CoreShapeAdapter.getType(e), e);
  }

  public static SymbolicExpression toUnknown(CRightHandSide rE) {
    if (rE instanceof CExpression) {
      return toUnknown((CExpression) rE);
    } else {
      CType type = CoreShapeAdapter.getType(rE);
      CExpression dummy = CIdExpression.DUMMY_ID(type);
      return toUnknown(dummy);
    }
  }

  private static SymbolicExpression toConstant(ShapeValue val, CExpression e) {
    return new ConstantSE(val, CoreShapeAdapter.getType(e), e);
  }

  public static SymbolicExpression toConstant(ShapeValue val, CRightHandSide rE) {
    if (rE instanceof CExpression) {
      return toConstant(val, (CExpression) rE);
    } else {
      CType type = CoreShapeAdapter.getType(rE);
      CExpression dummy = CIdExpression.DUMMY_ID(type);
      return toConstant(val, dummy);
    }
  }

  public static SymbolicExpression toConstant(ShapeValue val, CType t) {
    return toConstant(val, CIdExpression.DUMMY_ID(t));
  }

  /**
   * We perform casting while encapsulating an explicit value into a symbolic expression. For
   * example, when we try to wrap -1 as a UNSIGNED_INT constant symbolic expression, then we
   * should first convert -1 as UNSIGNED_INT (i.e. 0xffffffff).
   */
  public static SymbolicExpression toConstantWithConversion(
      KnownExplicitValue expValue,
      CSimpleType t,
      MachineModel machineModel) {
    Value v = new NumericValue(expValue.getValue());
    v = AbstractExpressionValueVisitor.castCValue(v, t, machineModel, null, FileLocation.DUMMY);
    if (v.isNumericValue()) {
      KnownExplicitValue result = KnownExplicitValue.of(v.asNumericValue().getNumber());
      return toConstant(result, t);
    } else {
      return toUnknown(t);
    }
  }

  public static boolean isExplicit(SymbolicExpression se) {
    return se.getValueKind() == SymbolicKind.EXPLICIT;
  }

  /**
   * Construct a symbolic assignment X = E where X is the specified symbolic value and E is the
   * specified symbolic expression.
   */
  public static SymbolicExpression bind(KnownSymbolicValue sv, SymbolicExpression se) {
    SymbolicExpression left = toConstant(sv, CIdExpression.DUMMY_ID(se.getType()));
    return new BinarySE(left, se, BinaryOperator.EQUALS, CNumericTypes.INT, se.getOriginalExpression
        ());
  }

  /* *********************** */
  /* expression constructors */
  /* *********************** */

  public static SymbolicExpression plus(
      SymbolicExpression se1, SymbolicExpression se2, CType
      pType, MachineModel pModel) {
    CType type = CoreShapeAdapter.getType(pType);
    if (se1 instanceof ConstantSE) {
      if (se1.getValueKind() == SymbolicKind.UNKNOWN) {
        return toUnknown(type);
      } else if (se1.getValueKind() == SymbolicKind.EXPLICIT) {
        if (se2 instanceof ConstantSE && se2.getValueKind() == SymbolicKind.EXPLICIT) {
          assert (type instanceof CSimpleType);
          KnownExplicitValue ev1 = (KnownExplicitValue) se1.getValue();
          KnownExplicitValue ev2 = (KnownExplicitValue) se2.getValue();
          ShapeExplicitValue ev = ev1.add(ev2);
          Value v = new NumericValue(ev.getValue());
          v = AbstractExpressionValueVisitor.castCValue(v, type, pModel, null, FileLocation.DUMMY);
          if (v.isNumericValue()) {
            KnownExplicitValue sum = KnownExplicitValue.of(v.asNumericValue().getNumber());
            return toConstant(sum, type);
          } else {
            return toUnknown(type);
          }
        }
      }
    }
    if (se2 instanceof ConstantSE && se2.getValueKind() == SymbolicKind.UNKNOWN) {
      return toUnknown(type);
    }
    return new BinarySE(se1, se2, BinaryOperator.PLUS, type, CIdExpression.DUMMY_ID(type));
  }

  /**
   * Construct plus symbolic expression without casting values of operands.
   */
  public static SymbolicExpression makeBinary(
      SymbolicExpression se1, SymbolicExpression se2,
      BinaryOperator operator, CType pType) {
    return new BinarySE(se1, se2, operator, pType, CIdExpression.DUMMY_ID(pType));
  }

  /**
   * Multiply two symbolic expressions.
   */
  public static SymbolicExpression multiply(
      SymbolicExpression se1, SymbolicExpression se2,
      CType pType, MachineModel pModel) {
    CType type = CoreShapeAdapter.getType(pType);
    if (se1 instanceof ConstantSE) {
      if (se1.getValueKind() == SymbolicKind.UNKNOWN) {
        return toUnknown(type);
      } else if (se1.getValueKind() == SymbolicKind.EXPLICIT) {
        if (se2 instanceof ConstantSE && se2.getValueKind() == SymbolicKind.EXPLICIT) {
          // two operands are explicit values, then we can reduce them into a new explicit product
          assert (type instanceof CSimpleType);
          KnownExplicitValue ev1 = (KnownExplicitValue) se1.getValue();
          KnownExplicitValue ev2 = (KnownExplicitValue) se2.getValue();
          ShapeExplicitValue ev = ev1.multiply(ev2);
          Value v = new NumericValue(ev.getValue());
          v = AbstractExpressionValueVisitor.castCValue(v, type, pModel, null, FileLocation
              .DUMMY);
          if (v.isNumericValue()) {
            KnownExplicitValue product = KnownExplicitValue.of(v.asNumericValue().getNumber());
            return toConstant(product, type);
          } else {
            return toUnknown(type);
          }
        }
      }
    }
    if (se2 instanceof ConstantSE && se2.getValueKind() == SymbolicKind.UNKNOWN) {
      return toUnknown(type);
    }
    return new BinarySE(se1, se2, BinaryOperator.MULTIPLY, type, CIdExpression.DUMMY_ID(type));
  }

  /**
   * Construct a less-than relational constraint for two symbolic expressions.
   */
  public static SymbolicExpression lessThan(SymbolicExpression se1, SymbolicExpression se2) {
    if (isUnknown(se1) || isUnknown(se2)) {
      return toUnknown(CNumericTypes.INT);
    }
    return new BinarySE(se1, se2, BinaryOperator.LESS_THAN, CNumericTypes.INT, CIdExpression
        .DUMMY_ID(CNumericTypes.INT));
  }

  public static SymbolicExpression greaterEqual(SymbolicExpression se1, SymbolicExpression se2) {
    if (isUnknown(se1) || isUnknown(se2)) {
      return toUnknown(CNumericTypes.INT);
    }
    return new BinarySE(se1, se2, BinaryOperator.GREATER_EQUAL, CNumericTypes.INT, CIdExpression
        .DUMMY_ID(CNumericTypes.INT));
  }

  /* ********* */
  /* converter */
  /* ********* */

  public static SymbolicExpression convertTo(
      SymbolicExpression se, CType type, MachineModel
      pModel) {
    if (se instanceof ConstantSE) {
      if (se.getValueKind() == SymbolicKind.EXPLICIT) {
        KnownExplicitValue expValue = (KnownExplicitValue) se.getValue();
        Value v = new NumericValue(expValue.getValue());
        v = AbstractExpressionValueVisitor.castCValue(v, type, pModel, null, FileLocation.DUMMY);
        if (v.isNumericValue()) {
          KnownExplicitValue newValue = KnownExplicitValue.of(v.asNumericValue().getNumber());
          return toConstant(newValue, type);
        } else {
          return toUnknown(type);
        }
      }
      return new ConstantSE(se.getValue(), type, se.getOriginalExpression());
    } else if (se instanceof BinarySE) {
      return new BinarySE(((BinarySE) se).getOperand1(), ((BinarySE) se).getOperand2(), (
          (BinarySE) se).getOperator(), type, se.getOriginalExpression());
    } else if (se instanceof UnarySE) {
      return new UnarySE(((UnarySE) se).getOperand(), ((UnarySE) se).getOperator(), type, se
          .getOriginalExpression());
    } else if (se instanceof CastSE) {
      return SymbolicExpressionVisitor.simplifyCast((CastSE) se, type, se.getOriginalExpression()
          , pModel);
    } else {
      throw new IllegalStateException("unsupported symbolic expression kind");
    }
  }

}
