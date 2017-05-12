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
package org.sosy_lab.cpachecker.cpa.shape.visitors.results;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicExpression;

public final class DeclaredTypeData {

  private ShapeState state;
  private boolean containsVLA;
  private SymbolicExpression size;
  private CType trueType;

  private DeclaredTypeData(
      ShapeState pState, boolean pIsVLA, SymbolicExpression pSize,
      CType pType) {
    state = pState;
    containsVLA = pIsVLA;
    size = pSize;
    trueType = pType;
  }

  public static DeclaredTypeData of(
      ShapeState pState, boolean pVLA, SymbolicExpression pSize,
      CType pType) {
    return new DeclaredTypeData(pState, pVLA, pSize, pType);
  }

  public ShapeState getState() {
    return state;
  }

  public boolean isContainsVLA() {
    return containsVLA;
  }

  public SymbolicExpression getSize() {
    return size;
  }

  public CType getTrueType() {
    return trueType;
  }

}
