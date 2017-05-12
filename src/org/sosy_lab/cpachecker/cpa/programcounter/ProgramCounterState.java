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
package org.sosy_lab.cpachecker.cpa.programcounter;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import java.math.BigInteger;
import java.util.Set;


public class ProgramCounterState
    implements AbstractState, LatticeAbstractState<ProgramCounterState> {

  private static final ProgramCounterState TOP = new ProgramCounterState();

  private final Set<BigInteger> values;

  private ProgramCounterState() {
    this(null);
  }

  private ProgramCounterState(ImmutableSet<BigInteger> pValues) {
    assert pValues != null || TOP == null;
    this.values = pValues;
  }

  public boolean isTop() {
    assert values != null || this == TOP;
    return values == null;
  }

  public boolean isBottom() {
    return !isTop() && values.isEmpty();
  }

  public boolean containsValue(BigInteger pValue) {
    return isTop() || this.values.contains(pValue);
  }

  @Override
  public boolean isLessOrEqual(ProgramCounterState other) {
    return other.containsAll(this);
  }

  public boolean containsAll(ProgramCounterState pOther) {
    if (pOther.isTop()) {
      return isTop();
    }
    if (isTop() || this == pOther) {
      return true;
    }
    return values.containsAll(pOther.values);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO instanceof ProgramCounterState) {
      ProgramCounterState other = (ProgramCounterState) pO;
      return values == other.values || values != null && values.equals(other.values);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(values);
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }

  @Override
  public String toString() {
    return isTop() ? "TOP" : values.toString();
  }

  public ProgramCounterState insert(BigInteger pValue) {
    if (containsValue(pValue)) {
      return this;
    }
    return new ProgramCounterState(
        ImmutableSet.<BigInteger>builder().addAll(values).add(pValue).build());
  }

  @Override
  public ProgramCounterState join(ProgramCounterState pOther) {
    if (isTop() || pOther.isTop()) {
      return TOP;
    }
    ImmutableSet.Builder<BigInteger> builder = null;
    for (BigInteger value : pOther.values) {
      if (!containsValue(value)) {
        if (builder == null) {
          builder = ImmutableSet.builder();
          builder.addAll(values);
        }
        builder.add(value);
      }
    }
    if (builder == null) {
      return this;
    }
    return new ProgramCounterState(builder.build());
  }

  public ProgramCounterState remove(BigInteger pValue) {
    if (isTop() || !containsValue(pValue)) {
      return this;
    }
    ImmutableSet.Builder<BigInteger> builder = ImmutableSet.builder();
    for (BigInteger value : values) {
      if (!value.equals(pValue)) {
        builder.add(value);
      }
    }
    return new ProgramCounterState(builder.build());
  }

  public static ProgramCounterState getTopState() {
    return TOP;
  }

  public static ProgramCounterState getStateForValue(BigInteger pPCValue) {
    return new ProgramCounterState(ImmutableSet.of(pPCValue));
  }

  public static AbstractState getStateForValues(Iterable<BigInteger> pValues) {
    ImmutableSet.Builder<BigInteger> builder = ImmutableSet.builder();
    for (BigInteger value : pValues) {
      builder.add(value);
    }
    return new ProgramCounterState(builder.build());
  }

}
