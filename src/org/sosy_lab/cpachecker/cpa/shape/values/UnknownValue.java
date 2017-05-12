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
package org.sosy_lab.cpachecker.cpa.shape.values;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;

/**
 * Unknown value which is eligible for various kinds of values.
 */
public class UnknownValue implements ShapeSymbolicValue, ShapeExplicitValue, ShapeAddressValue {

  private static final UnknownValue INSTANCE = new UnknownValue();

  @Override
  public String toString() {
    return "UNKNOWN";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !(obj instanceof UnknownValue)) {
      return false;
    }
    // there is only one unknown value instance
    return true;
  }

  public static UnknownValue getInstance() {
    return INSTANCE;
  }

  @Override
  public boolean isUnknown() {
    return true;
  }

  @Override
  public Number getValue() {
    throw new IllegalStateException("unknown value has no concrete value");
  }

  @Override
  public int getAsInt() {
    throw new IllegalStateException("unknown value has no concrete value");
  }

  @Override
  public long getAsLong() {
    throw new IllegalStateException("unknown value has no concrete value");
  }

  @Override
  public float getAsFloat() {
    throw new IllegalStateException("unknown value has no concrete value");
  }

  @Override
  public double getAsDouble() {
    throw new IllegalStateException("unknown value has no concrete value");
  }

  @Override
  public ShapeExplicitValue add(ShapeExplicitValue pRValue) {
    return INSTANCE;
  }

  @Override
  public ShapeExplicitValue subtract(ShapeExplicitValue pRValue) {
    return INSTANCE;
  }

  @Override
  public ShapeExplicitValue multiply(ShapeExplicitValue pRValue) {
    return INSTANCE;
  }

  @Override
  public ShapeExplicitValue divide(ShapeExplicitValue pRValue) {
    return INSTANCE;
  }

  @Override
  public ShapeExplicitValue shiftLeft(ShapeExplicitValue pRValue) {
    return INSTANCE;
  }

  @Override
  public ShapeExplicitValue shiftRight(ShapeExplicitValue pRValue) {
    return INSTANCE;
  }

  @Override
  public ShapeExplicitValue castValue(CType pType, MachineModel pMachineModel) {
    return INSTANCE;
  }

  @Override
  public ShapeExplicitValue getOffset() {
    return INSTANCE;
  }

  @Override
  public SGObject getObject() {
    return null;
  }

  @Override
  public Address getAddress() {
    return Address.UNKNOWN;
  }
}
