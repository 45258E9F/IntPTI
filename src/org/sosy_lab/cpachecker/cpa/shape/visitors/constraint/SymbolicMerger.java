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
package org.sosy_lab.cpachecker.cpa.shape.visitors.constraint;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cpa.shape.constraint.BinarySE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.CastSE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.ConstantSE;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicKind;
import org.sosy_lab.cpachecker.cpa.shape.constraint.UnarySE;
import org.sosy_lab.cpachecker.cpa.shape.util.ReducerResult;
import org.sosy_lab.cpachecker.cpa.shape.values.KnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeSymbolicValue;
import org.sosy_lab.cpachecker.cpa.shape.values.UnknownValue;

import java.util.Set;

/**
 * Merge multiple symbolic values by their identity relation.
 * Note: the target value should be kept while source values should be replaced with target value.
 */
public class SymbolicMerger implements SEVisitor<ReducerResult> {

  private final KnownSymbolicValue target;
  private final Set<KnownSymbolicValue> sources;

  public SymbolicMerger(KnownSymbolicValue pTarget, Set<KnownSymbolicValue> pSources) {
    target = pTarget;
    sources = pSources;
    // sanity check: target value should not in the set of source values
    if (sources.contains(target)) {
      sources.remove(target);
    }
  }

  @Override
  public ReducerResult visit(ConstantSE pValue) {
    if (pValue.getValueKind() == SymbolicKind.SYMBOLIC) {
      KnownSymbolicValue symbolic = (KnownSymbolicValue) pValue.getValue();
      if (sources.contains(symbolic)) {
        // this symbolic identifier should be replaced with the target one
        ConstantSE newSE = new ConstantSE(target, pValue.getType(), pValue.getOriginalExpression());
        return ReducerResult.of(true, newSE);
      }
    }
    return ReducerResult.of(false, pValue);
  }

  @Override
  public ReducerResult visit(BinarySE pValue) {
    ReducerResult lResult = pValue.getOperand1().accept(this);
    ReducerResult rResult = pValue.getOperand2().accept(this);
    if (!lResult.getChangeFlag() && !rResult.getChangeFlag()) {
      return ReducerResult.of(false, pValue);
    }
    SymbolicExpression newOp1 = (SymbolicExpression) lResult.getExpression();
    SymbolicExpression newOp2 = (SymbolicExpression) rResult.getExpression();
    // Since the symbolic merger does not contain state information, we do not need to interpret
    // symbolic values into address values or anything else.
    // We only need to handle the following cases:
    // (1) operation on the two identical symbolic values;
    // (2) symbolic operation involving ZERO (if we replace some symbolic values with ZERO)
    SymbolicExpression newSE;
    if (newOp1 instanceof ConstantSE && newOp1.getValueKind() == SymbolicKind.SYMBOLIC &&
        newOp2 instanceof ConstantSE && newOp2.getValueKind() == SymbolicKind.SYMBOLIC) {
      ShapeSymbolicValue result = handleBinaryOperation((KnownSymbolicValue) newOp1.getValue(),
          (KnownSymbolicValue) newOp2.getValue(), pValue.getOperator());
      if (!result.isUnknown()) {
        newSE = new ConstantSE(result, pValue.getType(), pValue.getOriginalExpression());
        return ReducerResult.of(true, newSE);
      }
    }
    newSE = new BinarySE(newOp1, newOp2, pValue.getOperator(), pValue.getType(), pValue
        .getOriginalExpression());
    return ReducerResult.of(true, newSE);
  }

  @Override
  public ReducerResult visit(UnarySE pValue) {
    ReducerResult subResult = pValue.getOperand().accept(this);
    if (!subResult.getChangeFlag()) {
      return ReducerResult.of(false, pValue);
    }
    SymbolicExpression subOp = (SymbolicExpression) subResult.getExpression();
    UnaryOperator operator = pValue.getOperator();
    if (subOp instanceof ConstantSE && subOp.getValueKind() == SymbolicKind.SYMBOLIC) {
      KnownSymbolicValue op = (KnownSymbolicValue) subOp.getValue();
      if (operator == UnaryOperator.MINUS) {
        if (op.equals(KnownSymbolicValue.ZERO)) {
          return ReducerResult.of(true, new ConstantSE(KnownSymbolicValue.ZERO, pValue.getType(),
              pValue.getOriginalExpression()));
        }
      }
    }
    UnarySE newSE = new UnarySE(subOp, operator, pValue.getType(), pValue.getOriginalExpression());
    return ReducerResult.of(true, newSE);
  }

  @Override
  public ReducerResult visit(CastSE pValue) {
    ReducerResult subResult = pValue.getOperand().accept(this);
    if (!subResult.getChangeFlag()) {
      return ReducerResult.of(false, pValue);
    }
    SymbolicExpression subOp = (SymbolicExpression) subResult.getExpression();
    if (subOp instanceof ConstantSE && subOp.getValueKind() == SymbolicKind.SYMBOLIC) {
      KnownSymbolicValue op = (KnownSymbolicValue) subOp.getValue();
      if (op.equals(KnownSymbolicValue.ZERO)) {
        return ReducerResult.of(true, new ConstantSE(KnownSymbolicValue.ZERO, pValue.getType(),
            pValue.getOriginalExpression()));
      }
    }
    CastSE newSE = new CastSE(subOp, pValue.getType(), pValue.getOriginalExpression());
    return ReducerResult.of(true, newSE);
  }

  /* ***************** */
  /* auxiliary methods */
  /* ***************** */

  /**
   * A simple reducer for symbolic binary operation.
   */
  static ShapeSymbolicValue handleBinaryOperation(
      KnownSymbolicValue op1, KnownSymbolicValue
      op2, BinaryOperator operator) {
    boolean isZero;
    switch (operator) {
      case EQUALS:
      case LESS_EQUAL:
      case GREATER_EQUAL:
        isZero = op1.equals(op2);
        return isZero ? KnownSymbolicValue.TRUE : UnknownValue.getInstance();
      case NOT_EQUALS:
      case LESS_THAN:
      case GREATER_THAN:
        isZero = op1.equals(op2);
        return isZero ? KnownSymbolicValue.FALSE : UnknownValue.getInstance();
      case PLUS:
      case SHIFT_LEFT:
      case BINARY_OR:
      case BINARY_XOR:
      case SHIFT_RIGHT:
        isZero = op1.equals(KnownSymbolicValue.ZERO) && op2.equals(KnownSymbolicValue.ZERO);
        return isZero ? KnownSymbolicValue.ZERO : UnknownValue.getInstance();
      case MINUS:
      case MODULO:
        isZero = op1.equals(op2);
        return isZero ? KnownSymbolicValue.ZERO : UnknownValue.getInstance();
      case DIVIDE:
        if (op2.equals(KnownSymbolicValue.ZERO)) {
          return UnknownValue.getInstance();
        }
        isZero = op1.equals(KnownSymbolicValue.ZERO);
        return isZero ? KnownSymbolicValue.ZERO : UnknownValue.getInstance();
      case MULTIPLY:
      case BINARY_AND:
        isZero = op1.equals(KnownSymbolicValue.ZERO) || op2.equals(KnownSymbolicValue.ZERO);
        return isZero ? KnownSymbolicValue.ZERO : UnknownValue.getInstance();
      default:
        return UnknownValue.getInstance();
    }
  }

}
