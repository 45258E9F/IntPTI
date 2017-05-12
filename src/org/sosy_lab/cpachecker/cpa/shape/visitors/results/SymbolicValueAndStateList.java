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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.cpa.shape.values.ShapeSymbolicValue;

import java.util.List;
import java.util.Random;

public class SymbolicValueAndStateList {

  private final List<? extends SymbolicValueAndState> valueAndStates;

  public SymbolicValueAndStateList(List<? extends SymbolicValueAndState> pList) {
    valueAndStates = ImmutableList.copyOf(pList);
  }

  public SymbolicValueAndStateList(SymbolicValueAndState pValueAndState) {
    valueAndStates = ImmutableList.of(pValueAndState);
  }

  public int size() {
    return valueAndStates.size();
  }

  @Override
  public String toString() {
    return valueAndStates.toString();
  }

  @Override
  public boolean equals(Object obj) {
    return !(obj == null || !(obj instanceof SymbolicValueAndStateList)) && valueAndStates
        .equals(((SymbolicValueAndStateList) obj).valueAndStates);
  }

  public List<? extends SymbolicValueAndState> asSymbolicValueAndStateList() {
    return valueAndStates;
  }

  public static SymbolicValueAndStateList of(SymbolicValueAndState pValueAndState) {
    return new SymbolicValueAndStateList(pValueAndState);
  }

  public static SymbolicValueAndStateList of(ShapeState pState) {
    return of(SymbolicValueAndState.of(pState));
  }

  public static SymbolicValueAndStateList of(ShapeState pState, ShapeSymbolicValue pValue) {
    return of(SymbolicValueAndState.of(pState, pValue));
  }

  public static SymbolicValueAndStateList copyOfValueList(List<SymbolicValueAndState> pList) {
    return new SymbolicValueAndStateList(pList);
  }

  public Optional<? extends SymbolicValueAndState> getOneElement() {
    if (valueAndStates.isEmpty()) {
      return Optional.absent();
    }
    Random random = new Random();
    int index = random.nextInt(valueAndStates.size());
    return Optional.of(valueAndStates.get(index));
  }

}
