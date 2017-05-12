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
package org.sosy_lab.cpachecker.cpa.invariants.formula;

import com.google.common.base.Preconditions;

/**
 * This is just a plain formula with two operands and one operator.
 */
public abstract class AbstractBinaryFormula<ConstantType> extends AbstractFormula<ConstantType> {

  private final NumeralFormula<ConstantType> operand1;

  private final NumeralFormula<ConstantType> operand2;

  private final String operator;

  // isCommutative is TRUE for "=", "+", "*" and FALSE for "-", "/", "<".
  private final boolean isCommutative;

  /**
   * Creates a new formula with two operands.
   *
   * @param pOperand1 the first operand.
   * @param pOperand2 the second operand.
   */
  protected AbstractBinaryFormula(
      String pOperator,
      boolean pIsCommutative,
      NumeralFormula<ConstantType> pOperand1,
      NumeralFormula<ConstantType> pOperand2) {
    super(pOperand1.getBitVectorInfo());
    Preconditions.checkNotNull(pOperator);
    Preconditions.checkNotNull(pOperand1);
    Preconditions.checkNotNull(pOperand1);
    Preconditions.checkArgument(pOperand1.getBitVectorInfo().equals(pOperand2.getBitVectorInfo()));
    this.operator = pOperator;
    this.isCommutative = pIsCommutative;
    this.operand1 = pOperand1;
    this.operand2 = pOperand2;
  }

  public NumeralFormula<ConstantType> getOperand1() {
    return this.operand1;
  }

  public NumeralFormula<ConstantType> getOperand2() {
    return this.operand2;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (this == o) {
      return true;
    }
    if (this.getClass().equals(o.getClass())) { // equality for subclasses
      AbstractBinaryFormula<?> other = (AbstractBinaryFormula<?>) o;
      if (!getBitVectorInfo().equals(other.getBitVectorInfo())) {
        return false;
      }
      if (operator.equals(other.operator) && isCommutative == other.isCommutative) {
        if (isCommutative) {
          return
              getOperand1().equals(other.getOperand1()) && getOperand2().equals(other.getOperand2())
                  || getOperand1().equals(other.getOperand2()) && getOperand2()
                  .equals(other.getOperand1());
        } else {
          return getOperand1().equals(other.getOperand1()) && getOperand2()
              .equals(other.getOperand2());
        }
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 31 * operator.hashCode()
        + getOperand1().hashCode() * getOperand2().hashCode()
        + 43 * getBitVectorInfo().hashCode();
  }

  @Override
  public String toString() {
    return String.format("(%s %s %s)", getOperand1(), operator, getOperand2());
  }

}
