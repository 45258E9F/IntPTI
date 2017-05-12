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

import com.google.common.base.Preconditions;

import java.math.BigInteger;
import java.util.Objects;


public class CompoundBitVectorIntervalManager implements CompoundIntervalManager {

  private final BitVectorInfo info;

  private final boolean allowSignedWrapAround;

  private final OverflowEventHandler overflowEventHandler;

  public CompoundBitVectorIntervalManager(
      BitVectorInfo pInfo,
      boolean pAllowSignedWrapAround,
      OverflowEventHandler pOverflowEventHandler) {
    Preconditions.checkNotNull(pInfo);
    this.info = pInfo;
    this.allowSignedWrapAround = pAllowSignedWrapAround;
    this.overflowEventHandler = pOverflowEventHandler;
  }

  @Override
  public int hashCode() {
    return Objects.hash(info, allowSignedWrapAround);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof CompoundBitVectorIntervalManager) {
      CompoundBitVectorIntervalManager other = (CompoundBitVectorIntervalManager) pOther;
      return allowSignedWrapAround == other.allowSignedWrapAround
          && info.equals(other.info);
    }
    return false;
  }

  @Override
  public CompoundInterval allPossibleValues() {
    return CompoundBitVectorInterval.of(info.getRange());
  }

  @Override
  public CompoundInterval bottom() {
    return CompoundBitVectorInterval.bottom(info);
  }

  @Override
  public CompoundInterval logicalFalse() {
    return CompoundBitVectorInterval.logicalFalse(info);
  }

  @Override
  public CompoundInterval logicalTrue() {
    return CompoundBitVectorInterval.logicalTrue(info);
  }

  @Override
  public CompoundInterval union(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.unionWith(operand2);
  }

  @Override
  public boolean contains(CompoundInterval pContainer, CompoundInterval pElement) {
    checkOperands(pContainer, pElement);
    CompoundBitVectorInterval container = (CompoundBitVectorInterval) pContainer;
    CompoundBitVectorInterval element = (CompoundBitVectorInterval) pElement;
    return container.contains(element);
  }

  @Override
  public CompoundInterval lessEqual(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.lessEqual(operand2);
  }

  @Override
  public CompoundInterval lessThan(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.lessThan(operand2);
  }

  @Override
  public CompoundInterval greaterEqual(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.greaterEqual(operand2);
  }

  @Override
  public CompoundInterval greaterThan(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.greaterThan(operand2);
  }

  @Override
  public CompoundInterval singleton(long pValue) {
    return CompoundBitVectorInterval.singleton(info, pValue);
  }

  @Override
  public CompoundInterval singleton(BigInteger pValue) {
    return CompoundBitVectorInterval.singleton(info, pValue);
  }

  @Override
  public CompoundInterval castedSingleton(BigInteger pValue) {
    return CompoundBitVectorInterval
        .cast(info, pValue, pValue, allowSignedWrapAround, OverflowEventHandler.EMPTY);
  }

  @Override
  public CompoundInterval fromBoolean(boolean pValue) {
    return CompoundBitVectorInterval.fromBoolean(info, pValue);
  }

  @Override
  public CompoundInterval intersect(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.intersectWith(operand2);
  }

  @Override
  public CompoundInterval modulo(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.modulo(operand2, allowSignedWrapAround, overflowEventHandler);
  }

  @Override
  public CompoundInterval add(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.add(operand2, allowSignedWrapAround, overflowEventHandler);
  }

  @Override
  public CompoundInterval logicalEquals(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.logicalEquals(operand2);
  }

  @Override
  public CompoundInterval binaryAnd(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.binaryAnd(operand2, allowSignedWrapAround, overflowEventHandler);
  }

  @Override
  public CompoundInterval binaryOr(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.binaryOr(operand2, allowSignedWrapAround, overflowEventHandler);
  }

  @Override
  public CompoundInterval binaryXor(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.binaryXor(operand2, allowSignedWrapAround, overflowEventHandler);
  }

  @Override
  public CompoundInterval binaryNot(CompoundInterval pOperand) {
    checkOperand(pOperand);
    CompoundBitVectorInterval operand = (CompoundBitVectorInterval) pOperand;
    return operand.binaryNot(allowSignedWrapAround, overflowEventHandler);
  }

  @Override
  public CompoundInterval divide(CompoundInterval pNumerator, CompoundInterval pDenominator) {
    checkOperands(pNumerator, pDenominator);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pNumerator;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pDenominator;
    return operand1.divide(operand2, allowSignedWrapAround, overflowEventHandler);
  }

  @Override
  public CompoundInterval multiply(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.multiply(operand2, allowSignedWrapAround, overflowEventHandler);
  }

  @Override
  public CompoundInterval shiftLeft(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.shiftLeft(operand2, allowSignedWrapAround, overflowEventHandler);
  }

  @Override
  public CompoundInterval shiftRight(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.shiftRight(operand2, allowSignedWrapAround, overflowEventHandler);
  }

  @Override
  public boolean doIntersect(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.intersectsWith(operand2);
  }

  @Override
  public CompoundInterval span(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return CompoundBitVectorInterval.span(operand1, operand2);
  }

  @Override
  public CompoundInterval negate(CompoundInterval pToNegate) {
    checkOperand(pToNegate);
    return ((CompoundBitVectorInterval) pToNegate)
        .negate(allowSignedWrapAround, overflowEventHandler);
  }

  @Override
  public CompoundInterval cast(BitVectorInfo pInfo, CompoundInterval pToCast) {
    checkOperand(pToCast);
    return ((CompoundBitVectorInterval) pToCast)
        .cast(pInfo, allowSignedWrapAround, OverflowEventHandler.EMPTY);
  }

  private static void checkOperand(CompoundInterval pOperand) {
    if (!(pOperand instanceof CompoundBitVectorInterval)) {
      throw new IllegalArgumentException("Operand is not a compound bit vector interval.");
    }
  }

  private static void checkOperands(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperand(pOperand1);
    checkOperand(pOperand2);
  }

}
