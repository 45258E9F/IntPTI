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

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeValue;
import org.sosy_lab.cpachecker.cpa.shape.values.UnknownValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.constraint.CRVisitor;
import org.sosy_lab.cpachecker.cpa.shape.visitors.constraint.SEVisitor;

import java.util.Objects;

/**
 * Unary symbolic expression, representing expressions with one operand.
 */
public class UnarySE implements SymbolicExpression {

  private final SymbolicExpression operand;
  private final UnaryOperator operator;
  private final CType type;
  private final CExpression origExp;

  public UnarySE(
      SymbolicExpression pOperand, UnaryOperator pOp, CType pType, CExpression
      pOrigExp) {
    operand = pOperand;
    operator = pOp;
    type = pType;
    origExp = pOrigExp;
  }

  public SymbolicExpression getOperand() {
    return operand;
  }

  public UnaryOperator getOperator() {
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
    CExpression op = operand.getSymbolicExpression();
    if (op == null) {
      return null;
    }
    return new CUnaryExpression(origExp.getFileLocation(), type, op, operator);
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
    return Objects.hash(operand, operator, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof UnarySE)) {
      return false;
    }
    UnarySE that = (UnarySE) obj;
    return operand.equals(that.operand) && operator == that.operator && type.getCanonicalType()
        .equals(that.type.getCanonicalType());
  }

  @Override
  public String toString() {
    CExpression ce = getSymbolicExpression();
    return ce == null ? "NULL" : ce.toString();
  }
}
