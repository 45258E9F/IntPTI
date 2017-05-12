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
import org.sosy_lab.cpachecker.cpa.invariants.BitVectorType;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundBitVectorInterval;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundIntervalManager;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundIntervalManagerFactory;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundMathematicalInterval;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;

/**
 * Instances of this class are visitors for compound state invariants formulae
 * which are used to evaluate the visited formulae to compound states. This
 * visitor deliberately uses a weaker evaluation strategy than a
 * {@link FormulaCompoundStateEvaluationVisitor} in order to enable the CPA
 * strategy to prevent infeasible interpretation of the analyzed code.
 */
public class FormulaAbstractionVisitor extends
                                       DefaultParameterizedNumeralFormulaVisitor<CompoundInterval, Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>>, CompoundInterval>
    implements FormulaEvaluationVisitor<CompoundInterval> {

  private final FormulaCompoundStateEvaluationVisitor evaluationVisitor;

  private final CompoundIntervalManagerFactory compoundIntervalManagerFactory;

  public FormulaAbstractionVisitor(CompoundIntervalManagerFactory pCompoundIntervalManagerFactory) {
    evaluationVisitor = new FormulaCompoundStateEvaluationVisitor(pCompoundIntervalManagerFactory);
    compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
  }

  private CompoundIntervalManager getCompoundIntervalManager(BitVectorInfo pBitVectorInfo) {
    return compoundIntervalManagerFactory.createCompoundIntervalManager(pBitVectorInfo);
  }

  private CompoundIntervalManager getCompoundIntervalManager(BitVectorType pBitvectorType) {
    return getCompoundIntervalManager(pBitvectorType.getBitVectorInfo());
  }

  @Override
  public CompoundInterval visit(
      Add<CompoundInterval> pAdd,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return weakAdd(pAdd.getBitVectorInfo(),
        pAdd.getSummand1().accept(evaluationVisitor, pEnvironment),
        pAdd.getSummand2().accept(evaluationVisitor, pEnvironment));
  }

  @Override
  public CompoundInterval visit(
      Constant<CompoundInterval> pConstant,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return pConstant.getValue();
  }

  @Override
  public CompoundInterval visit(
      Multiply<CompoundInterval> pMultiply,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return weakMultiply(pMultiply.getBitVectorInfo(),
        pMultiply.getFactor1().accept(this, pEnvironment),
        pMultiply.getFactor2().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(
      ShiftLeft<CompoundInterval> pShiftLeft,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    CompoundInterval toShift = pShiftLeft.getShifted().accept(this, pEnvironment);
    CompoundInterval shiftDistance = pShiftLeft.getShiftDistance().accept(this, pEnvironment);
    CompoundInterval evaluation =
        getCompoundIntervalManager(pShiftLeft).shiftLeft(toShift, shiftDistance);
    if (!shiftDistance.containsPositive()) {
      return evaluation;
    }
    return abstractionOf(pShiftLeft.getBitVectorInfo(), evaluation);
  }

  @Override
  public CompoundInterval visit(
      Variable<CompoundInterval> pVariable,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    NumeralFormula<CompoundInterval> varState = pEnvironment.get(pVariable.getMemoryLocation());
    if (varState == null) {
      return getCompoundIntervalManager(pVariable).allPossibleValues();
    }
    return varState.accept(this, pEnvironment);
  }

  @Override
  protected CompoundInterval visitDefault(
      NumeralFormula<CompoundInterval> pFormula,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pParam) {
    return abstractionOf(pFormula.getBitVectorInfo(), pFormula.accept(evaluationVisitor, pParam));
  }

  private CompoundInterval abstractionOf(BitVectorInfo pBitVectorInfo, CompoundInterval pValue) {
    if (pValue.isBottom() || pValue.containsAllPossibleValues()) {
      return pValue;
    }
    CompoundInterval result = pValue.signum();
    boolean extendToNeg = false;
    CompoundIntervalManager compoundIntervalManager = getCompoundIntervalManager(pBitVectorInfo);
    if (!compoundIntervalManager.lessThan(pValue, result).isDefinitelyFalse()) {
      extendToNeg = true;
    }
    if (!compoundIntervalManager.greaterThan(pValue, result).isDefinitelyFalse()) {
      result = result.extendToMaxValue();
    }
    if (extendToNeg) {
      result = result.extendToMinValue();
    }
    assert compoundIntervalManager.union(result, pValue).equals(result);
    return result;
  }

  /**
   * Compute a compound state representing possible results of adding the
   * given summand compound states up. This method provides a much weaker
   * implementation of compound state addition than using the methods from
   * {@link CompoundBitVectorInterval} or {@link CompoundMathematicalInterval}
   * and will thus usually return a much larger result range.
   *
   * @param pA the first summand.
   * @param pB the second summand.
   * @return a state representing possible results of adding the given summand compound states up.
   */
  private CompoundInterval weakAdd(
      BitVectorInfo pBitVectorInfo,
      CompoundInterval pA,
      CompoundInterval pB) {
    if (pA.isSingleton() && pA.contains(BigInteger.ZERO)) {
      return pB;
    }
    if (pB.isSingleton() && pB.contains(BigInteger.ZERO)) {
      return pA;
    }
    return abstractionOf(pBitVectorInfo, getCompoundIntervalManager(pBitVectorInfo).add(pA, pB));
  }

  /**
   * Compute a compound state representing possible results of multiplying the
   * given factor compound states. This method provides a much weaker
   * implementation of compound state addition than using the methods from
   * {@link CompoundBitVectorInterval} or {@link CompoundMathematicalInterval}
   * and will thus usually return a much larger result range.
   *
   * @param a the first factor.
   * @param b the second factor.
   * @return a state representing possible results of multiplying the given factor compound states.
   */
  private CompoundInterval weakMultiply(
      BitVectorInfo pBitVectorInfo,
      CompoundInterval a,
      CompoundInterval b) {
    if (a.isSingleton() && a.contains(BigInteger.ZERO)) {
      return a;
    }
    if (b.isSingleton() && b.contains(BigInteger.ZERO)) {
      return b;
    }
    if (a.isSingleton() && a.contains(BigInteger.ONE)) {
      return b;
    }
    if (b.isSingleton() && b.contains(BigInteger.ONE)) {
      return a;
    }
    CompoundIntervalManager cim = getCompoundIntervalManager(pBitVectorInfo);
    if (a.isSingleton() && a.contains(BigInteger.ONE.negate())) {
      return cim.negate(b);
    }
    if (b.isSingleton() && b.contains(BigInteger.ONE.negate())) {
      return cim.negate(a);
    }
    return abstractionOf(pBitVectorInfo, cim.multiply(a, b));
  }

  @Override
  public BooleanConstant<CompoundInterval> visit(
      Equal<CompoundInterval> pEqual,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluationVisitor.visit(pEqual, pEnvironment);
  }

  @Override
  public BooleanConstant<CompoundInterval> visit(
      LessThan<CompoundInterval> pLessThan,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluationVisitor.visit(pLessThan, pEnvironment);
  }

  @Override
  public BooleanConstant<CompoundInterval> visit(
      LogicalAnd<CompoundInterval> pAnd,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluationVisitor.visit(pAnd, pEnvironment);
  }

  @Override
  public BooleanConstant<CompoundInterval> visit(
      LogicalNot<CompoundInterval> pNot,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluationVisitor.visit(pNot, pEnvironment);
  }

  @Override
  public BooleanConstant<CompoundInterval> visitFalse(
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluationVisitor.visitFalse(pEnvironment);
  }

  @Override
  public BooleanConstant<CompoundInterval> visitTrue(
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluationVisitor.visitTrue(pEnvironment);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.evaluationVisitor, this.compoundIntervalManagerFactory);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof FormulaAbstractionVisitor) {
      FormulaAbstractionVisitor other = (FormulaAbstractionVisitor) pOther;
      return evaluationVisitor.equals(other.evaluationVisitor)
          && compoundIntervalManagerFactory.equals(other.compoundIntervalManagerFactory);
    }
    return false;
  }

}
