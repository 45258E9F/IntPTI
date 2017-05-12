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
package org.sosy_lab.cpachecker.cpa.shape.merge.abs;

import org.sosy_lab.cpachecker.cpa.shape.constraint.ConstraintRepresentation;

import java.util.Set;

public class GuardedValuePair implements AbstractionPair<GuardedValue> {

  private GuardedValue left;
  private GuardedValue right;

  private GuardedValuePair(
      long l, long r, Set<ConstraintRepresentation> pLeftGuards,
      Set<ConstraintRepresentation> pRightGuards) {
    left = GuardedValue.of(l, pLeftGuards);
    right = GuardedValue.of(r, pRightGuards);
  }

  private GuardedValuePair(GuardedValue pL, GuardedValue pR) {
    left = pL;
    right = pR;
  }

  public static GuardedValuePair of(
      long l, long r,
      Set<ConstraintRepresentation> pLeftGuards,
      Set<ConstraintRepresentation> pRightGuards) {
    return new GuardedValuePair(l, r, pLeftGuards, pRightGuards);
  }

  public static GuardedValuePair of(GuardedValue pL, GuardedValue pR) {
    return new GuardedValuePair(pL, pR);
  }

  @Override
  public GuardedValue getLeft() {
    return left;
  }

  @Override
  public GuardedValue getRight() {
    return right;
  }

  @Override
  public Long getLeftValue() {
    return left.getValue();
  }

  @Override
  public Long getRightValue() {
    return right.getValue();
  }

  @Override
  public void updateLeftValue(Long v) {
    left = GuardedValue.of(v, left);
  }

  @Override
  public void updateRightValue(Long v) {
    right = GuardedValue.of(v, right);
  }

  public void addMoreGuards(Set<ConstraintRepresentation> pSes) {
    left.addMoreGuards(pSes);
    right.addMoreGuards(pSes);
  }

  @Override
  public String toString() {
    return "LEFT: " + left.toString() + "\nRIGHT: " + right.toString();
  }
}
