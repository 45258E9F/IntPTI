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

import com.google.common.base.Preconditions;

import java.math.BigInteger;

/**
 * A symbolic value has no numerical semantics. It is just a symbol, or presentation of a certain
 * value such as memory address.
 */
public class KnownSymbolicValue extends ShapeKnownValue implements ShapeSymbolicValue {

  protected KnownSymbolicValue(BigInteger pValue) {
    super(pValue);
  }

  public static final KnownSymbolicValue ZERO = new KnownSymbolicValue(BigInteger.ZERO);

  public static final KnownSymbolicValue ONE = new KnownSymbolicValue(BigInteger.ONE);

  /**
   * Note that address values are always positive.
   * TRUE is symbolic -1, which is compatible with other symbolic values.
   */
  public static final KnownSymbolicValue TRUE = new KnownSymbolicValue(BigInteger.valueOf(-1));

  public static final KnownSymbolicValue FALSE = ZERO;

  public static KnownSymbolicValue valueOf(long pValue) {
    if (pValue == 0) {
      return ZERO;
    } else if (pValue == 1) {
      return ONE;
    } else {
      return new KnownSymbolicValue(BigInteger.valueOf(pValue));
    }
  }

  public static KnownSymbolicValue valueOf(BigInteger pValue) {
    Preconditions.checkNotNull(pValue);

    if (pValue.equals(BigInteger.ZERO)) {
      return ZERO;
    } else if (pValue.equals(BigInteger.ONE)) {
      return ONE;
    } else {
      return new KnownSymbolicValue(pValue);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof KnownSymbolicValue)) {
      return false;
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + super.hashCode();
    return result;
  }

}
