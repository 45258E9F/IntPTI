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
package org.sosy_lab.cpachecker.util.predicates.ldd;

import org.sosy_lab.common.NativeLibraries;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;

import java.util.Collection;

public class LDDRegionManager {

  static {
    NativeLibraries.loadLibrary("JLDD");
  }

  private final LDDFactory factory;

  private final LDDRegion trueFormula;
  private final LDDRegion falseFormula;

  public LDDRegionManager(int size) {
    this.factory = new LDDFactory(size);
    this.trueFormula = new LDDRegion(factory.one());
    this.falseFormula = new LDDRegion(factory.zero());
  }

  public boolean entails(Region pF1, Region pF2) {
    LDDRegion f1 = (LDDRegion) pF1;
    LDDRegion f2 = (LDDRegion) pF2;
    LDD imp = f1.getLDD().imp(f2.getLDD());
    return imp.isOne();
  }

  public LDDRegion makeTrue() {
    return trueFormula;
  }

  public LDDRegion makeFalse() {
    return falseFormula;
  }

  public LDDRegion makeNot(Region pF) {
    LDDRegion f = (LDDRegion) pF;
    return new LDDRegion(f.getLDD().negate());
  }

  public LDDRegion makeAnd(Region pF1, Region pF2) {
    LDDRegion f1 = (LDDRegion) pF1;
    LDDRegion f2 = (LDDRegion) pF2;
    return new LDDRegion(f1.getLDD().and(f2.getLDD()));
  }

  public LDDRegion makeOr(Region pF1, Region pF2) {
    LDDRegion f1 = (LDDRegion) pF1;
    LDDRegion f2 = (LDDRegion) pF2;
    return new LDDRegion(f1.getLDD().or(f2.getLDD()));
  }

  public LDDRegion makeExists(Region pF1, Region pF2) {
    LDDRegion f1 = (LDDRegion) pF1;
    LDDRegion f2 = (LDDRegion) pF2;
    return new LDDRegion(f1.getLDD().exists(f2.getLDD()));
  }

  public Region getIfThenElse(
      Region conditionRegion,
      Region positiveRegion,
      Region negativeRegion) {
    LDDRegion condition = (LDDRegion) conditionRegion;
    LDDRegion positive = (LDDRegion) positiveRegion;
    LDDRegion negative = (LDDRegion) negativeRegion;
    return new LDDRegion(condition.getLDD().makeIfThenElse(positive.getLDD(), negative.getLDD()));
  }

  public LDDRegion makeConstantAssignment(
      Collection<Pair<Integer, Integer>> varIndices,
      int varCount,
      int constValue) {
    return new LDDRegion(this.factory.makeConstantAssignment(varIndices, varCount, constValue));
  }

  public LDDRegion makeNode(
      Collection<Pair<Integer, Integer>> varCoeffs,
      int varCount,
      boolean leq,
      int constant) {
    return new LDDRegion(this.factory.makeNode(varCoeffs, varCount, leq, constant));
  }

  public LDDRegion makeXor(LDDRegion pAssumeToRegion, LDDRegion pAssumeToRegion2) {
    return new LDDRegion(pAssumeToRegion.getLDD().xor(pAssumeToRegion2.getLDD()));
  }

  public LDDRegion replace(
      Integer pInteger,
      Collection<Pair<Integer, Integer>> pIndexCoefficients,
      int varCount,
      int pConstant,
      LDDRegion pRegion) {
    return new LDDRegion(
        this.factory.replace(pRegion.getLDD(), pInteger, pIndexCoefficients, varCount, pConstant));
  }

}
