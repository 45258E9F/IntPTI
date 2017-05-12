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
package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Objects;

/**
 * A binary {@link SymbolicExpression}.
 * Represents all <code>SymbolicExpression</code>s that consist of two operands.
 */
public abstract class BinarySymbolicExpression extends SymbolicExpression {

  private static final long serialVersionUID = -5708374107141557273L;

  private final SymbolicExpression operand1;
  private final SymbolicExpression operand2;

  /**
   * {@link Type} the operands are cast to during calculation.
   */
  private final Type calculationType;

  /**
   * {@link Type} of the binary expression
   */
  private Type expressionType;

  protected BinarySymbolicExpression(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pExpressionType,
      Type pCalculationType
  ) {
    operand1 = pOperand1;
    operand2 = pOperand2;
    expressionType = pExpressionType;
    calculationType = pCalculationType;
  }

  protected BinarySymbolicExpression(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pExpressionType,
      Type pCalculationType,
      MemoryLocation pRepresentedLocation
  ) {

    super(pRepresentedLocation);
    operand1 = pOperand1;
    operand2 = pOperand2;
    expressionType = pExpressionType;
    calculationType = pCalculationType;
  }

  @Override
  public Type getType() {
    return expressionType;
  }

  public Type getCalculationType() {
    return calculationType;
  }

  public SymbolicExpression getOperand1() {
    return operand1;
  }

  public SymbolicExpression getOperand2() {
    return operand2;
  }

  @Override
  public boolean isTrivial() {
    return operand1.isTrivial() && operand2.isTrivial();
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj == null || getClass() != pObj.getClass()) {
      return false;
    }

    BinarySymbolicExpression that = (BinarySymbolicExpression) pObj;

    return super.equals(that) &&
        operand1.equals(that.operand1) && operand2.equals(that.operand2) && expressionType
        .equals(that.expressionType);
  }

  @Override
  public int hashCode() {
    return super.hashCode() + Objects.hash(getClass(), operand1, operand2, expressionType);
  }

  @Override
  public String getRepresentation() {
    if (getRepresentedLocation().isPresent()) {
      return getRepresentedLocation().get().toString();

    } else {
      return "(" + operand1.getRepresentation() + " " + getOperationString()
          + " " + operand2.getRepresentation() + ")";
    }
  }

  @Override
  public String toString() {
    return operand1 + " " + getOperationString() + " " + operand2;
  }

  public abstract String getOperationString();
}
