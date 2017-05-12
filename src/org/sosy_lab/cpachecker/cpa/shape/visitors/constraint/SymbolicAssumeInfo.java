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
package org.sosy_lab.cpachecker.cpa.shape.visitors.constraint;

import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.constraint.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.shape.util.AssumeEvaluator;
import org.sosy_lab.cpachecker.util.Triple;

import java.util.ArrayList;
import java.util.List;

public final class SymbolicAssumeInfo {

  private final List<Triple<SymbolicExpression, AssumeEvaluator, ShapeState>> list = new
      ArrayList<>();

  private SymbolicAssumeInfo() {
  }

  private SymbolicAssumeInfo(SymbolicExpression se, ShapeState state) {
    Triple<SymbolicExpression, AssumeEvaluator, ShapeState> newTriple = Triple.of(se, null, state);
    list.add(newTriple);
  }

  public static SymbolicAssumeInfo of() {
    return new SymbolicAssumeInfo();
  }

  public static SymbolicAssumeInfo of(SymbolicExpression se, ShapeState state) {
    return new SymbolicAssumeInfo(se, state);
  }

  public void add(SymbolicExpression se, ShapeState state) {
    Triple<SymbolicExpression, AssumeEvaluator, ShapeState> newTriple = Triple.of(se, null, state);
    list.add(newTriple);
  }

  public void add(SymbolicExpression se, AssumeEvaluator pEvaluator, ShapeState state) {
    Triple<SymbolicExpression, AssumeEvaluator, ShapeState> newTriple = Triple.of(se, pEvaluator,
        state);
    list.add(newTriple);
  }

  public int size() {
    return list.size();
  }

  public SymbolicExpression getSymbolicExpression(int i) {
    if (i < 0 || i >= list.size()) {
      throw new IllegalStateException("Invalid index value");
    }
    return list.get(i).getFirst();
  }

  public AssumeEvaluator getAssumeEvaluator(int i) {
    if (i < 0 || i >= list.size()) {
      throw new IllegalStateException("Invalid index value");
    }
    return list.get(i).getSecond();
  }

  public ShapeState getState(int i) {
    if (i < 0 || i >= list.size()) {
      throw new IllegalStateException("Invalid index value");
    }
    return list.get(i).getThird();
  }

}
