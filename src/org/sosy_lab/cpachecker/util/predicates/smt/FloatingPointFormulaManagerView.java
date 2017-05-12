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

import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.FloatingPointFormula;
import org.sosy_lab.solver.api.FloatingPointFormulaManager;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.api.FormulaType.FloatingPointType;

import java.math.BigDecimal;


public class FloatingPointFormulaManagerView
    extends BaseManagerView
    implements FloatingPointFormulaManager {

  private final FloatingPointFormulaManager manager;

  FloatingPointFormulaManagerView(
      FormulaWrappingHandler pWrappingHandler,
      FloatingPointFormulaManager pManager) {
    super(pWrappingHandler);
    this.manager = pManager;
  }

  @Override
  public <T extends Formula> T castTo(FloatingPointFormula pNumber, FormulaType<T> pTargetType) {
    // This method needs to unwrap/wrap pTargetType and the return value,
    // in case they are replaced with other formula types.
    return wrap(pTargetType, manager.castTo(pNumber, unwrapType(pTargetType)));
  }

  @Override
  public FloatingPointFormula castFrom(
      Formula pNumber,
      boolean pSigned,
      FloatingPointType pTargetType) {
    // This method needs to unwrap pNumber,
    // in case it is replaced with another formula type.
    return manager.castFrom(unwrap(pNumber), pSigned, pTargetType);
  }

  @Override
  public FloatingPointFormula negate(FloatingPointFormula pNumber) {
    return manager.negate(pNumber);
  }

  @Override
  public FloatingPointFormula add(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.add(pNumber1, pNumbe2);
  }

  @Override
  public FloatingPointFormula subtract(
      FloatingPointFormula pNumber1,
      FloatingPointFormula pNumbe2) {
    return manager.subtract(pNumber1, pNumbe2);
  }

  @Override
  public FloatingPointFormula divide(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.divide(pNumber1, pNumbe2);
  }

  @Override
  public FloatingPointFormula multiply(
      FloatingPointFormula pNumber1,
      FloatingPointFormula pNumbe2) {
    return manager.multiply(pNumber1, pNumbe2);
  }

  @Override
  public BooleanFormula assignment(FloatingPointFormula pNumber1, FloatingPointFormula pNumber2) {
    return manager.assignment(pNumber1, pNumber2);
  }

  @Override
  public BooleanFormula equalWithFPSemantics(
      FloatingPointFormula pNumber1,
      FloatingPointFormula pNumbe2) {
    return manager.equalWithFPSemantics(pNumber1, pNumbe2);
  }

  @Override
  public BooleanFormula greaterThan(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.greaterThan(pNumber1, pNumbe2);
  }

  @Override
  public BooleanFormula greaterOrEquals(
      FloatingPointFormula pNumber1,
      FloatingPointFormula pNumbe2) {
    return manager.greaterOrEquals(pNumber1, pNumbe2);
  }

  @Override
  public BooleanFormula lessThan(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.lessThan(pNumber1, pNumbe2);
  }

  @Override
  public BooleanFormula lessOrEquals(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.lessOrEquals(pNumber1, pNumbe2);
  }

  @Override
  public BooleanFormula isNaN(FloatingPointFormula pNumber) {
    return manager.isNaN(pNumber);
  }

  @Override
  public BooleanFormula isInfinity(FloatingPointFormula pNumber) {
    return manager.isInfinity(pNumber);
  }

  @Override
  public BooleanFormula isZero(FloatingPointFormula pNumber) {
    return manager.isZero(pNumber);
  }

  @Override
  public BooleanFormula isSubnormal(FloatingPointFormula pNumber) {
    return manager.isSubnormal(pNumber);
  }

  @Override
  public FloatingPointFormula makeNumber(double pN, FormulaType.FloatingPointType type) {
    return manager.makeNumber(pN, type);
  }

  @Override
  public FloatingPointFormula makeNumber(BigDecimal pN, FormulaType.FloatingPointType type) {
    return manager.makeNumber(pN, type);
  }

  @Override
  public FloatingPointFormula makeNumber(String pN, FormulaType.FloatingPointType type) {
    return manager.makeNumber(pN, type);
  }

  @Override
  public FloatingPointFormula makeNumber(Rational pN, FormulaType.FloatingPointType type) {
    return manager.makeNumber(pN, type);
  }

  @Override
  public FloatingPointFormula makeVariable(String pVar, FormulaType.FloatingPointType pType) {
    return manager.makeVariable(pVar, pType);
  }

  public FloatingPointFormula makeVariable(
      String pVar,
      int idx,
      FormulaType.FloatingPointType pType) {
    return manager.makeVariable(FormulaManagerView.makeName(pVar, idx), pType);
  }

  @Override
  public FloatingPointFormula makePlusInfinity(FloatingPointType pType) {
    return manager.makePlusInfinity(pType);
  }

  @Override
  public FloatingPointFormula makeMinusInfinity(FloatingPointType pType) {
    return manager.makeMinusInfinity(pType);
  }

  @Override
  public FloatingPointFormula makeNaN(FloatingPointType pType) {
    return manager.makeNaN(pType);
  }
}