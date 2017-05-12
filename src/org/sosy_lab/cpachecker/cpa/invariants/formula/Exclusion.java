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


public class Exclusion<ConstantType> extends AbstractFormula<ConstantType>
    implements NumeralFormula<ConstantType> {

  private final NumeralFormula<ConstantType> excluded;

  private Exclusion(NumeralFormula<ConstantType> pExcluded) {
    super(pExcluded.getBitVectorInfo());
    this.excluded = pExcluded;
  }

  public NumeralFormula<ConstantType> getExcluded() {
    return this.excluded;
  }

  @Override
  public String toString() {
    return String.format("\\?(%s)", getExcluded());
  }

  @Override
  public int hashCode() {
    return ~getExcluded().hashCode();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO instanceof Exclusion) {
      return getExcluded().equals(((Exclusion<?>) pO).getExcluded());
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

  public static <ConstantType> NumeralFormula<ConstantType> of(NumeralFormula<ConstantType> pToExclude) {
    return new Exclusion<>(pToExclude);
  }

}
