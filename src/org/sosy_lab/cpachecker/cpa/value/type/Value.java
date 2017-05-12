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
package org.sosy_lab.cpachecker.cpa.value.type;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.types.c.CType;

import java.io.Serializable;


/**
 * Base class for values that can be tracked by the ValueAnalysisCPA.
 *
 * Traditionally, ValueAnalysisCPA would only keep track of long type values.
 * For the future, floats, symbolic values, and SMG nodes should
 * also be supported.
 */
public interface Value extends Serializable {
  public boolean isNumericValue();

  /**
   * True if we have no idea about this value(can not track it), false otherwise.
   */
  public boolean isUnknown();

  /**
   * True if we deterministically know the actual value, false otherwise.
   */
  public boolean isExplicitlyKnown();

  /**
   * Returns the NumericValue if the stored value can be explicitly represented
   * by a numeric value, null otherwise.
   **/
  public NumericValue asNumericValue();

  /**
   * Return the long value if this is a long value, null otherwise.
   **/
  public Long asLong(CType type);

  /**
   * Singleton class used to signal that the value is unknown (could be anything).
   **/
  public static final class UnknownValue implements Value, Serializable {

    private static final long serialVersionUID = -300842115868319184L;
    private static final UnknownValue instance = new UnknownValue();

    @Override
    public String toString() {
      return "UNKNOWN";
    }

    public static UnknownValue getInstance() {
      return instance;
    }

    @Override
    public boolean isNumericValue() {
      return false;
    }

    @Override
    public NumericValue asNumericValue() {
      return null;
    }

    @Override
    public Long asLong(CType type) {
      checkNotNull(type);
      return null;
    }

    @Override
    public boolean isUnknown() {
      return true;
    }

    @Override
    public boolean isExplicitlyKnown() {
      return false;
    }

    protected Object readResolve() {
      return instance;
    }

  }
}
