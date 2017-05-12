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
package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import java.util.Objects;

public class CBinaryExpression extends ABinaryExpression implements CExpression {

  private final CType calculationType;

  public CBinaryExpression(
      final FileLocation pFileLocation,
      final CType pExpressionType,
      final CType pCalculationType,
      final CExpression pOperand1,
      final CExpression pOperand2,
      final BinaryOperator pOperator) {
    super(pFileLocation, pExpressionType, pOperand1, pOperand2, pOperator);
    calculationType = pCalculationType;
  }

  @Override
  public <R, X extends Exception> R accept(CExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
  }

  /**
   * This method returns the type for the 'calculation' of this binary expression.
   *
   * This is not the type of the 'result' of this binary expression.
   * The result-type is returned from getType().
   * <p>
   * Before the calculation, if necessary,
   * both operand should be casted to the calculation-type.
   * In most cases this is a widening.
   *
   * Then the operation is performed in this type.
   * This may cause an overflow, if the calculation-type is not big enough.
   *
   * After the calculation, if necessary,
   * the result of the binary operation should be casted to the result-type.
   */
  public CType getCalculationType() {
    return calculationType;
  }

  @Override
  public CExpression getOperand1() {
    return (CExpression) super.getOperand1();
  }

  @Override
  public CExpression getOperand2() {
    return (CExpression) super.getOperand2();
  }

  @Override
  public BinaryOperator getOperator() {
    return (BinaryOperator) super.getOperator();
  }

  public static enum BinaryOperator implements ABinaryExpression.ABinaryOperator {
    MULTIPLY("*"),
    DIVIDE("/"),
    MODULO("%"),
    PLUS("+"),
    MINUS("-"),
    SHIFT_LEFT("<<"),
    SHIFT_RIGHT(">>"),
    LESS_THAN("<"),
    GREATER_THAN(">"),
    LESS_EQUAL("<="),
    GREATER_EQUAL(">="),
    BINARY_AND("&"),
    BINARY_XOR("^"),
    BINARY_OR("|"),
    EQUALS("=="),
    NOT_EQUALS("!="),;

    private final String op;

    private BinaryOperator(String pOp) {
      op = pOp;
    }

    /**
     * Returns the string representation of this operator (e.g. "*", "+").
     */
    @Override
    public String getOperator() {
      return op;
    }

    public boolean isLogicalOperator() {
      switch (this) {
        case MULTIPLY:
        case DIVIDE:
        case MODULO:
        case PLUS:
        case MINUS:
        case SHIFT_LEFT:
        case SHIFT_RIGHT:
        case BINARY_AND:
        case BINARY_OR:
        case BINARY_XOR:
          return false;
        case LESS_EQUAL:
        case LESS_THAN:
        case GREATER_EQUAL:
        case GREATER_THAN:
        case EQUALS:
        case NOT_EQUALS:
          return true;
        default:
          throw new AssertionError("Unhandled case statement");
      }
    }

    public boolean isBitShiftOperator() {
      switch (this) {
        case SHIFT_LEFT:
        case SHIFT_RIGHT:
          return true;
        default:
          return false;
      }
    }

    public BinaryOperator getOppositeLogicalOperator() {
      assert isLogicalOperator();
      switch (this) {
        case LESS_EQUAL:
          return GREATER_THAN;
        case LESS_THAN:
          return GREATER_EQUAL;
        case GREATER_EQUAL:
          return LESS_THAN;
        case GREATER_THAN:
          return LESS_EQUAL;
        case EQUALS:
          return NOT_EQUALS;
        case NOT_EQUALS:
          return EQUALS;
        default:
          return this;
      }
    }

    public BinaryOperator getReversedLogicalOperator() {
      assert isLogicalOperator();
      switch (this) {
        case LESS_EQUAL:
          return GREATER_EQUAL;
        case LESS_THAN:
          return GREATER_THAN;
        case GREATER_EQUAL:
          return LESS_EQUAL;
        case GREATER_THAN:
          return LESS_THAN;
        case EQUALS:
          return EQUALS;
        case NOT_EQUALS:
          return NOT_EQUALS;
        default:
          return this;
      }
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(calculationType);
    return result * prime + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CBinaryExpression)) {
      return false;
    }

    final CBinaryExpression other = (CBinaryExpression) obj;

    return Objects.equals(other.calculationType, calculationType) && super.equals(obj);
  }
}
