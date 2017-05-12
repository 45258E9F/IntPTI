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
package org.sosy_lab.cpachecker.cpa.invariants.formula;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInfo;
import org.sosy_lab.cpachecker.cpa.invariants.BitVectorType;

import java.util.Objects;

/**
 * Instances of this class represent constants within invariants formulae.
 *
 * @param <T> the type of the constant value.
 */
public class Constant<T> extends AbstractFormula<T> implements NumeralFormula<T> {

  /**
   * The value of the constant.
   */
  private final T value;

  /**
   * Creates a new constant with the given value.
   *
   * @param pValue the value of the constant.
   */
  private Constant(BitVectorInfo pInfo, T pValue) {
    super(pInfo);
    Preconditions.checkNotNull(pValue);
    if (pValue instanceof BitVectorType) {
      Preconditions.checkArgument(pInfo.equals(((BitVectorType) pValue).getBitVectorInfo()));
    }
    this.value = pValue;
  }

  /**
   * Gets the value of the constant.
   *
   * @return the value of the constant.
   */
  public T getValue() {
    return this.value;
  }

  @Override
  public String toString() {
    return getValue().toString();
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof Constant) {
      Constant<?> other = (Constant<?>) pOther;
      return getBitVectorInfo().equals(other.getBitVectorInfo())
          && getValue().equals(other.getValue());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getBitVectorInfo(), getValue());
  }

  @Override
  public <ReturnType> ReturnType accept(NumeralFormulaVisitor<T, ReturnType> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public <ReturnType, ParamType> ReturnType accept(
      ParameterizedNumeralFormulaVisitor<T, ParamType, ReturnType> pVisitor, ParamType pParameter) {
    return pVisitor.visit(this, pParameter);
  }

  /**
   * Gets a invariants formula representing a constant with the given value.
   *
   * @param pValue the value of the constant.
   * @return a invariants formula representing a constant with the given value.
   */
  static <T> Constant<T> of(BitVectorInfo pInfo, T pValue) {
    return new Constant<>(pInfo, pValue);
  }

  /**
   * Gets a invariants formula representing a constant with the given value.
   *
   * @param pValue the value of the constant.
   * @return a invariants formula representing a constant with the given value.
   */
  static <T extends BitVectorType> Constant<T> of(T pValue) {
    return new Constant<>(pValue.getBitVectorInfo(), pValue);
  }

}
