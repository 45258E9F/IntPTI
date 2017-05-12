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
 * Instances of this class represent less-than inequations over invariants
 * formulae.
 *
 * @param <ConstantType> the type of the constants used in the formula.
 */
public class LessThan<ConstantType> implements BooleanFormula<ConstantType> {

  /**
   * The first operand.
   */
  private final NumeralFormula<ConstantType> operand1;

  /**
   * The second operand.
   */
  private final NumeralFormula<ConstantType> operand2;

  /**
   * Creates a new less-than inequation over the given operands.
   *
   * @param pOperand1 the left operand of the inequation.
   * @param pOperand2 the right operand of the inequation.
   */
  private LessThan(
      NumeralFormula<ConstantType> pOperand1,
      NumeralFormula<ConstantType> pOperand2) {
    Preconditions.checkArgument(pOperand1.getBitVectorInfo().equals(pOperand2.getBitVectorInfo()));
    this.operand1 = pOperand1;
    this.operand2 = pOperand2;
  }

  public NumeralFormula<ConstantType> getOperand1() {
    return operand1;
  }

  public NumeralFormula<ConstantType> getOperand2() {
    return operand2;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof LessThan) {
      LessThan<?> other = (LessThan<?>) o;
      return getOperand1().equals(other.getOperand1()) && getOperand2().equals(other.getOperand2());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 31 * getOperand1().hashCode() + getOperand2().hashCode();
  }

  @Override
  public String toString() {
    return String.format("(%s < %s)", getOperand1(), getOperand2());
  }

  @Override
  public <ReturnType> ReturnType accept(BooleanFormulaVisitor<ConstantType, ReturnType> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public <ReturnType, ParamType> ReturnType accept(
      ParameterizedBooleanFormulaVisitor<ConstantType, ParamType, ReturnType> pVisitor,
      ParamType pParameter) {
    return pVisitor.visit(this, pParameter);
  }

  /**
   * Gets an invariants formula representing a less-than inequation over the
   * given operands.
   *
   * @param pOperand1 the left operand of the inequation.
   * @param pOperand2 the right operand of the inequation.
   * @return an invariants formula representing a less-than inequation over the given operands.
   */
  static <ConstantType> LessThan<ConstantType> of(
      NumeralFormula<ConstantType> pOperand1,
      NumeralFormula<ConstantType> pOperand2) {
    return new LessThan<>(pOperand1, pOperand2);
  }

}
