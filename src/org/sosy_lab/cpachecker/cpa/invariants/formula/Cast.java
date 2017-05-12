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

import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInfo;

import java.util.Objects;

public class Cast<ConstantType> extends AbstractFormula<ConstantType> {

  private final NumeralFormula<ConstantType> casted;

  private Cast(BitVectorInfo pInfo, NumeralFormula<ConstantType> pCasted) {
    super(pInfo);
    this.casted = pCasted;
  }

  public NumeralFormula<ConstantType> getCasted() {
    return casted;
  }

  @Override
  public String toString() {
    return String.format("((%d%s) %s)",
        getBitVectorInfo().getSize(),
        getBitVectorInfo().isSigned() ? "" : "U",
        getCasted());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getBitVectorInfo(), getCasted());
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof Cast) {
      Cast<?> other = (Cast<?>) pOther;
      return getBitVectorInfo().equals(other.getBitVectorInfo())
          && getCasted().equals(other.getCasted());
    }
    return false;
  }

  @Override
  public <ReturnType> ReturnType accept(NumeralFormulaVisitor<ConstantType, ReturnType> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public <ReturnType, ParamType> ReturnType accept(
      ParameterizedNumeralFormulaVisitor<ConstantType, ParamType, ReturnType> pVisitor,
      ParamType pParameter) {
    return pVisitor.visit(this, pParameter);
  }

  public static <ConstantType> Cast<ConstantType> of(
      BitVectorInfo pBitVectorInfo,
      NumeralFormula<ConstantType> pCasted) {
    return new Cast<>(pBitVectorInfo, pCasted);
  }

}
