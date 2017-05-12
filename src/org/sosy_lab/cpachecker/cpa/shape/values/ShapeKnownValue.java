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
package org.sosy_lab.cpachecker.cpa.shape.values;

import static org.sosy_lab.cpachecker.cpa.shape.values.NumberKind.BIG_INT;

import com.google.common.base.Objects;

import java.math.BigInteger;

/**
 * The abstract class of all known values, including known symbolic value or explicit value.
 */
public abstract class ShapeKnownValue {

  private final Number value;

  private final NumberKind kind;

  protected ShapeKnownValue(Number pValue, NumberKind pKind) {
    value = pValue;
    kind = pKind;
  }

  protected ShapeKnownValue(BigInteger pValue) {
    value = pValue;
    kind = BIG_INT;
  }

  public final Number getValue() {
    return value;
  }

  public final int getAsInt() {
    return value.intValue();
  }

  public final long getAsLong() {
    return value.longValue();
  }

  public final float getAsFloat() {
    return value.floatValue();
  }

  public final double getAsDouble() {
    return value.doubleValue();
  }

  public final BigInteger getAsBigInteger() {
    if (kind == BIG_INT) {
      return (BigInteger) value;
    } else {
      return BigInteger.valueOf(value.longValue());
    }
  }

  public final NumberKind getKind() {
    return kind;
  }

  public boolean isUnknown() {
    return false;
  }

  @Override
  public String toString() {
    return value.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !(obj instanceof ShapeKnownValue)) {
      return false;
    }
    ShapeKnownValue otherValue = (ShapeKnownValue) obj;
    return kind == otherValue.kind && value.equals(otherValue.value);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value, kind);
  }
}
