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
package org.sosy_lab.cpachecker.cpa.formulaslicing;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

/**
 * Intermediate state: a formula describing all possible executions at a point.
 */
public class SlicingIntermediateState extends SlicingState {

  private final CFANode node;

  /**
   * Formula describing state-space.
   */
  private final PathFormula pathFormula;

  /**
   * Starting point for the formula
   */
  private final SlicingAbstractedState start;

  /**
   * Checking coverage
   */
  private transient SlicingIntermediateState mergedInto;

  private SlicingIntermediateState(
      CFANode pNode, PathFormula pPathFormula,
      SlicingAbstractedState pStart) {
    node = pNode;
    pathFormula = pPathFormula;
    start = pStart;
  }

  public static SlicingIntermediateState of(
      CFANode pNode,
      PathFormula pPathFormula,
      SlicingAbstractedState pStart) {
    return new SlicingIntermediateState(pNode, pPathFormula, pStart);
  }

  public CFANode getNode() {
    return node;
  }

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  public SlicingAbstractedState getAbstractParent() {
    return start;
  }

  /**
   * Coverage checking for intermediate states
   */
  public void setMergedInto(SlicingIntermediateState other) {
    mergedInto = other;
  }

  public boolean isMergedInto(SlicingIntermediateState other) {
    return mergedInto == other;
  }


  @Override
  public boolean isAbstracted() {
    return false;
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }
}
