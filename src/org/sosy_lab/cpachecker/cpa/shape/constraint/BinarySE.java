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

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeValue;
import org.sosy_lab.cpachecker.cpa.shape.values.UnknownValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.constraint.CRVisitor;
import org.sosy_lab.cpachecker.cpa.shape.visitors.constraint.SEVisitor;

import java.util.Objects;

/**
 * Binary symbolic expression, representing expressions with two operands.
 */
public class BinarySE implements SymbolicExpression {

  private final SymbolicExpression operand1;
  private final SymbolicExpression operand2;
  private final BinaryOperator operator;
  private final CType type;
  private final CExpression origExp;

  /**
   * Constructor of the binary symbolic expression
   *
   * @param pOperand1 symbolic expression of operand 1
   * @param pOperand2 symbolic expression of operand 2
   * @param pOp       binary operator
   * @param pType     the calculated type of binary expression
   * @param pOrigExp  the original expression that this symbolic expression corresponds to
   */
  public BinarySE(
      SymbolicExpression pOperand1, SymbolicExpression pOperand2, BinaryOperator pOp,
      CType pType, CExpression pOrigExp) {
    operand1 = pOperand1;
    operand2 = pOperand2;
    operator = pOp;
    type = pType;
    origExp = pOrigExp;
  }

  public SymbolicExpression getOperand1() {
    return operand1;
  }

  public SymbolicExpression getOperand2() {
    return operand2;
  }

  public BinaryOperator getOperator() {
    return operator;
  }

  /* **************** */
  /* override methods */
  /* **************** */

  @Override
  public ShapeValue getValue() {
    return UnknownValue.getInstance();
  }

  @Override
  public SymbolicKind getValueKind() {
    return SymbolicKind.UNKNOWN;
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
    CExpression op1 = operand1.getSymbolicExpression();
    CExpression op2 = operand2.getSymbolicExpression();
    if (op1 == null || op2 == null) {
      return null;
    }
    return new CBinaryExpression(origExp.getFileLocation(), type, type, op1, op2, operator);
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
    return Objects.hash(operand1, operand2, operator, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof BinarySE)) {
      return false;
    }
    BinarySE that = (BinarySE) obj;
    return operand1.equals(that.operand1) && operand2.equals(that.operand2) && operator == that
        .operator && type.getCanonicalType().equals(that.type.getCanonicalType());
  }

  @Override
  public String toString() {
    CExpression ce = getSymbolicExpression();
    return ce == null ? "NULL" : ce.toString();
  }
}
