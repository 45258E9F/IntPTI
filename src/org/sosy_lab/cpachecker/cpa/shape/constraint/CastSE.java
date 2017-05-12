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

import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeValue;
import org.sosy_lab.cpachecker.cpa.shape.values.UnknownValue;
import org.sosy_lab.cpachecker.cpa.shape.visitors.constraint.CRVisitor;
import org.sosy_lab.cpachecker.cpa.shape.visitors.constraint.SEVisitor;

import java.util.Objects;

/**
 * Symbolic expression that represents cast expression such as (T)E.
 */
public class CastSE implements SymbolicExpression {

  private final SymbolicExpression operand;
  private final CType type;
  private final CExpression origExp;

  public CastSE(SymbolicExpression pOperand, CType pType, CExpression pOrigExp) {
    operand = pOperand;
    type = pType;
    origExp = pOrigExp;
  }

  public SymbolicExpression getOperand() {
    return operand;
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
    return new CCastExpression(origExp.getFileLocation(), type, op);
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
    return Objects.hash(operand, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof CastSE)) {
      return false;
    }
    CastSE that = (CastSE) obj;
    return operand.equals(that.operand) && type.getCanonicalType().equals(that.type
        .getCanonicalType());
  }

  @Override
  public String toString() {
    CExpression ce = getSymbolicExpression();
    return (ce == null) ? "NULL" : ce.toString();
  }
}
