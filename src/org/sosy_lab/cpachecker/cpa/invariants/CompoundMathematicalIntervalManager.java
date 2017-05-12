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
package org.sosy_lab.cpachecker.cpa.invariants;

import java.math.BigInteger;


public enum CompoundMathematicalIntervalManager implements CompoundIntervalManager {

  INSTANCE;

  @Override
  public CompoundInterval allPossibleValues() {
    return CompoundMathematicalInterval.top();
  }

  @Override
  public CompoundInterval bottom() {
    return CompoundMathematicalInterval.bottom();
  }

  @Override
  public CompoundInterval logicalFalse() {
    return CompoundMathematicalInterval.logicalFalse();
  }

  @Override
  public CompoundInterval logicalTrue() {
    return CompoundMathematicalInterval.logicalTrue();
  }

  @Override
  public CompoundInterval union(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundMathematicalInterval operand1 = (CompoundMathematicalInterval) pOperand1;
    CompoundMathematicalInterval operand2 = (CompoundMathematicalInterval) pOperand2;
    return operand1.unionWith(operand2);
  }

  @Override
  public boolean contains(CompoundInterval pContainer, CompoundInterval pElement) {
    checkOperands(pContainer, pElement);
    CompoundMathematicalInterval container = (CompoundMathematicalInterval) pContainer;
    CompoundMathematicalInterval element = (CompoundMathematicalInterval) pElement;
    return container.contains(element);
  }

  @Override
  public CompoundInterval lessEqual(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundMathematicalInterval operand1 = (CompoundMathematicalInterval) pOperand1;
    CompoundMathematicalInterval operand2 = (CompoundMathematicalInterval) pOperand2;
    return operand1.lessEqual(operand2);
  }

  @Override
  public CompoundInterval lessThan(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundMathematicalInterval operand1 = (CompoundMathematicalInterval) pOperand1;
    CompoundMathematicalInterval operand2 = (CompoundMathematicalInterval) pOperand2;
    return operand1.lessThan(operand2);
  }

  @Override
  public CompoundInterval greaterEqual(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundMathematicalInterval operand1 = (CompoundMathematicalInterval) pOperand1;
    CompoundMathematicalInterval operand2 = (CompoundMathematicalInterval) pOperand2;
    return operand1.greaterEqual(operand2);
  }

  @Override
  public CompoundInterval greaterThan(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundMathematicalInterval operand1 = (CompoundMathematicalInterval) pOperand1;
    CompoundMathematicalInterval operand2 = (CompoundMathematicalInterval) pOperand2;
    return operand1.greaterThan(operand2);
  }

  @Override
  public CompoundInterval singleton(long pValue) {
    return CompoundMathematicalInterval.singleton(pValue);
  }

  @Override
  public CompoundInterval singleton(BigInteger pValue) {
    return CompoundMathematicalInterval.singleton(pValue);
  }

  @Override
  public CompoundInterval castedSingleton(BigInteger pValue) {
    return singleton(pValue);
  }

  @Override
  public CompoundInterval fromBoolean(boolean pValue) {
    return CompoundMathematicalInterval.fromBoolean(pValue);
  }

  @Override
  public CompoundInterval intersect(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundMathematicalInterval operand1 = (CompoundMathematicalInterval) pOperand1;
    CompoundMathematicalInterval operand2 = (CompoundMathematicalInterval) pOperand2;
    return operand1.intersectWith(operand2);
  }

  @Override
  public CompoundInterval add(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundMathematicalInterval operand1 = (CompoundMathematicalInterval) pOperand1;
    CompoundMathematicalInterval operand2 = (CompoundMathematicalInterval) pOperand2;
    return operand1.add(operand2);
  }

  @Override
  public CompoundInterval modulo(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundMathematicalInterval operand1 = (CompoundMathematicalInterval) pOperand1;
    CompoundMathematicalInterval operand2 = (CompoundMathematicalInterval) pOperand2;
    return operand1.modulo(operand2);
  }

  @Override
  public CompoundInterval logicalEquals(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundMathematicalInterval operand1 = (CompoundMathematicalInterval) pOperand1;
    CompoundMathematicalInterval operand2 = (CompoundMathematicalInterval) pOperand2;
    return operand1.logicalEquals(operand2);
  }

  @Override
  public CompoundInterval binaryAnd(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundMathematicalInterval operand1 = (CompoundMathematicalInterval) pOperand1;
    CompoundMathematicalInterval operand2 = (CompoundMathematicalInterval) pOperand2;
    return operand1.binaryAnd(operand2);
  }

  @Override
  public CompoundInterval binaryOr(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundMathematicalInterval operand1 = (CompoundMathematicalInterval) pOperand1;
    CompoundMathematicalInterval operand2 = (CompoundMathematicalInterval) pOperand2;
    return operand1.binaryOr(operand2);
  }

  @Override
  public CompoundInterval binaryXor(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundMathematicalInterval operand1 = (CompoundMathematicalInterval) pOperand1;
    CompoundMathematicalInterval operand2 = (CompoundMathematicalInterval) pOperand2;
    return operand1.binaryXor(operand2);
  }

  @Override
  public CompoundInterval binaryNot(CompoundInterval pOperand) {
    checkOperand(pOperand);
    CompoundMathematicalInterval operand = (CompoundMathematicalInterval) pOperand;
    return operand.binaryNot();
  }

  @Override
  public CompoundInterval divide(CompoundInterval pNumerator, CompoundInterval pDenominator) {
    checkOperands(pNumerator, pDenominator);
    CompoundMathematicalInterval operand1 = (CompoundMathematicalInterval) pNumerator;
    CompoundMathematicalInterval operand2 = (CompoundMathematicalInterval) pDenominator;
    return operand1.divide(operand2);
  }

  @Override
  public CompoundInterval multiply(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundMathematicalInterval operand1 = (CompoundMathematicalInterval) pOperand1;
    CompoundMathematicalInterval operand2 = (CompoundMathematicalInterval) pOperand2;
    return operand1.multiply(operand2);
  }

  @Override
  public CompoundInterval shiftLeft(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundMathematicalInterval operand1 = (CompoundMathematicalInterval) pOperand1;
    CompoundMathematicalInterval operand2 = (CompoundMathematicalInterval) pOperand2;
    return operand1.shiftLeft(operand2);
  }

  @Override
  public CompoundInterval shiftRight(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundMathematicalInterval operand1 = (CompoundMathematicalInterval) pOperand1;
    CompoundMathematicalInterval operand2 = (CompoundMathematicalInterval) pOperand2;
    return operand1.shiftRight(operand2);
  }

  @Override
  public boolean doIntersect(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundMathematicalInterval operand1 = (CompoundMathematicalInterval) pOperand1;
    CompoundMathematicalInterval operand2 = (CompoundMathematicalInterval) pOperand2;
    return operand1.intersectsWith(operand2);
  }

  @Override
  public CompoundInterval span(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundMathematicalInterval operand1 = (CompoundMathematicalInterval) pOperand1;
    CompoundMathematicalInterval operand2 = (CompoundMathematicalInterval) pOperand2;
    return CompoundMathematicalInterval.span(operand1, operand2);
  }

  @Override
  public CompoundInterval negate(CompoundInterval pToNegate) {
    checkOperand(pToNegate);
    return ((CompoundMathematicalInterval) pToNegate).negate();
  }

  @Override
  public CompoundInterval cast(BitVectorInfo pInfo, CompoundInterval pToCast) {
    checkOperand(pToCast);
    return ((CompoundMathematicalInterval) pToCast).cast(pInfo);
  }

  private static void checkOperand(CompoundInterval pOperand) {
    if (!(pOperand instanceof CompoundMathematicalInterval)) {
      throw new IllegalArgumentException("Operand is not a compound mathematical interval.");
    }
  }

  private static void checkOperands(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperand(pOperand1);
    checkOperand(pOperand2);
  }

}
