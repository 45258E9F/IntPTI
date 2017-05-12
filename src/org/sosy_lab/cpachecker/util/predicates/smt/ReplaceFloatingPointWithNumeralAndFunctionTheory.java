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
package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.api.FloatingPointFormula;
import org.sosy_lab.solver.api.FloatingPointFormulaManager;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.api.FormulaType.FloatingPointType;
import org.sosy_lab.solver.api.FunctionDeclaration;
import org.sosy_lab.solver.api.NumeralFormula;
import org.sosy_lab.solver.api.NumeralFormulaManager;
import org.sosy_lab.solver.api.UFManager;

import java.math.BigDecimal;

class ReplaceFloatingPointWithNumeralAndFunctionTheory<T extends NumeralFormula>
    extends BaseManagerView
    implements FloatingPointFormulaManager {

  private final BooleanFormulaManager booleanManager;
  private final UFManager functionManager;
  private final NumeralFormulaManager<? super T, T> numericFormulaManager;
  private final FormulaType<T> formulaType;

  private final FunctionDeclaration<BooleanFormula> isSubnormalUfDecl;
  private final T zero;
  private final T nanVariable;
  private final T plusInfinityVariable;
  private final T minusInfinityVariable;

  ReplaceFloatingPointWithNumeralAndFunctionTheory(
      FormulaWrappingHandler pWrappingHandler,
      NumeralFormulaManager<? super T, T> pReplacementManager,
      UFManager rawFunctionManager,
      BooleanFormulaManager pBooleaManager) {
    super(pWrappingHandler);
    numericFormulaManager = pReplacementManager;
    booleanManager = pBooleaManager;
    functionManager = rawFunctionManager;

    formulaType = numericFormulaManager.getFormulaType();
    isSubnormalUfDecl = functionManager.declareUF("__isSubnormal__", FormulaType.BooleanType,
        formulaType);

    zero = numericFormulaManager.makeNumber(0);
    nanVariable = numericFormulaManager.makeVariable("__NaN__");
    plusInfinityVariable = numericFormulaManager.makeVariable("__+Infinity__");
    minusInfinityVariable = numericFormulaManager.makeVariable("__-Infinity__");
  }

  @SuppressWarnings("unchecked")
  private T unwrap(FloatingPointFormula pNumber) {
    return (T) super.unwrap(pNumber);
  }

  @Override
  public <T2 extends Formula> T2 castTo(FloatingPointFormula pNumber, FormulaType<T2> pTargetType) {
    // This method needs to handle only wrapping of FloatingPointFormulas,
    // wrapping of other types is handled by FloatingPointFormulaManagerView.
    return genericCast(unwrap(pNumber), pTargetType);
  }

  @Override
  public FloatingPointFormula castFrom(
      Formula pNumber,
      boolean pSigned,
      FloatingPointType pTargetType) {
    // This method needs to handle only wrapping of FloatingPointFormulas,
    // wrapping of other types is handled by FloatingPointFormulaManagerView.
    return wrap(pTargetType, genericCast(pNumber, unwrapType(pTargetType)));
  }

  private <T2 extends Formula> T2 genericCast(Formula pNumber, FormulaType<T2> pTargetType) {
    // This method does not handle wrapping, it needs to be done by callers.
    checkArgument(!(pNumber instanceof WrappingFormula<?, ?>));
    FormulaType<?> type = getFormulaType(pNumber);

    if (type.equals(pTargetType)) {
      // both theories are represented with same type, so we can use the exact same formula
      @SuppressWarnings("unchecked")
      T2 result = (T2) pNumber;
      return result;
    } else {
      FunctionDeclaration<T2> castFunction = functionManager.declareUF(
          "__cast_" + type + "_to_" + pTargetType + "__",
          pTargetType, type);
      return functionManager.callUF(castFunction, ImmutableList.of(pNumber));
    }
  }

  @Override
  public FloatingPointFormula negate(FloatingPointFormula pNumber) {
    return wrap(getFormulaType(pNumber), numericFormulaManager.negate(unwrap(pNumber)));
  }

  @Override
  public FloatingPointFormula add(FloatingPointFormula pNumber1, FloatingPointFormula pNumber2) {
    return wrap(getFormulaType(pNumber1),
        numericFormulaManager.add(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @Override
  public FloatingPointFormula subtract(
      FloatingPointFormula pNumber1,
      FloatingPointFormula pNumber2) {
    return wrap(getFormulaType(pNumber1),
        numericFormulaManager.subtract(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @Override
  public FloatingPointFormula divide(FloatingPointFormula pNumber1, FloatingPointFormula pNumber2) {
    T number1 = unwrap(pNumber1);
    T number2 = unwrap(pNumber2);
    FormulaType<FloatingPointFormula> targetType = getFormulaType(pNumber1);
    if (number2.equals(zero)) {
      // literal 0 is a problem for some solvers as divisor
      return wrap(targetType,
          booleanManager.ifThenElse(
              numericFormulaManager.equal(number1, zero),
              nanVariable,
              booleanManager.ifThenElse(
                  numericFormulaManager.lessThan(number1, zero),
                  minusInfinityVariable,
                  plusInfinityVariable)));

    }
    return wrap(targetType, numericFormulaManager.divide(number1, number2));
  }

  @Override
  public FloatingPointFormula multiply(
      FloatingPointFormula pNumber1,
      FloatingPointFormula pNumber2) {
    return wrap(getFormulaType(pNumber1),
        numericFormulaManager.multiply(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @Override
  public BooleanFormula assignment(FloatingPointFormula pNumber1, FloatingPointFormula pNumber2) {
    return numericFormulaManager.equal(unwrap(pNumber1), unwrap(pNumber2));
  }

  @Override
  public BooleanFormula equalWithFPSemantics(
      FloatingPointFormula pNumber1,
      FloatingPointFormula pNumber2) {
    return numericFormulaManager.equal(unwrap(pNumber1), unwrap(pNumber2));
  }

  @Override
  public BooleanFormula greaterThan(FloatingPointFormula pNumber1, FloatingPointFormula pNumber2) {
    return numericFormulaManager.greaterThan(unwrap(pNumber1), unwrap(pNumber2));
  }

  @Override
  public BooleanFormula greaterOrEquals(
      FloatingPointFormula pNumber1,
      FloatingPointFormula pNumber2) {
    return numericFormulaManager.greaterOrEquals(unwrap(pNumber1), unwrap(pNumber2));
  }

  @Override
  public BooleanFormula lessThan(FloatingPointFormula pNumber1, FloatingPointFormula pNumber2) {
    return numericFormulaManager.lessThan(unwrap(pNumber1), unwrap(pNumber2));
  }

  @Override
  public BooleanFormula lessOrEquals(FloatingPointFormula pNumber1, FloatingPointFormula pNumber2) {
    return numericFormulaManager.lessOrEquals(unwrap(pNumber1), unwrap(pNumber2));
  }

  @Override
  public BooleanFormula isNaN(FloatingPointFormula pNumber) {
    return numericFormulaManager.equal(unwrap(pNumber), nanVariable);
  }

  @Override
  public BooleanFormula isInfinity(FloatingPointFormula pNumber) {
    T number = unwrap(pNumber);
    return booleanManager.or(
        numericFormulaManager.equal(number, plusInfinityVariable),
        numericFormulaManager.equal(number, minusInfinityVariable));
  }

  @Override
  public BooleanFormula isZero(FloatingPointFormula pNumber) {
    return numericFormulaManager.equal(unwrap(pNumber), numericFormulaManager.makeNumber(0));
  }

  @Override
  public BooleanFormula isSubnormal(FloatingPointFormula pNumber) {
    return functionManager.callUF(isSubnormalUfDecl,
        ImmutableList.of(unwrap(pNumber)));
  }

  @Override
  public FloatingPointFormula makeNumber(double pN, FormulaType.FloatingPointType type) {
    return wrap(type, numericFormulaManager.makeNumber(pN));
  }

  @Override
  public FloatingPointFormula makeNumber(BigDecimal pN, FormulaType.FloatingPointType type) {
    return wrap(type, numericFormulaManager.makeNumber(pN));
  }

  @Override
  public FloatingPointFormula makeNumber(String pN, FormulaType.FloatingPointType type) {
    return wrap(type, numericFormulaManager.makeNumber(pN));
  }

  @Override
  public FloatingPointFormula makeNumber(Rational n, FloatingPointType type) {
    return wrap(type, numericFormulaManager.makeNumber(n));
  }

  @Override
  public FloatingPointFormula makeVariable(String pVar, FormulaType.FloatingPointType pType) {
    return wrap(pType, numericFormulaManager.makeVariable(pVar));
  }

  @Override
  public FloatingPointFormula makePlusInfinity(FloatingPointType pType) {
    return wrap(pType, plusInfinityVariable);
  }

  @Override
  public FloatingPointFormula makeMinusInfinity(FloatingPointType pType) {
    return wrap(pType, minusInfinityVariable);
  }

  @Override
  public FloatingPointFormula makeNaN(FloatingPointType pType) {
    return wrap(pType, nanVariable);
  }
}