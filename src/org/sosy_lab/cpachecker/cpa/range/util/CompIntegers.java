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
package org.sosy_lab.cpachecker.cpa.range.util;

import static org.sosy_lab.cpachecker.cpa.range.CompInteger.IntegerStatus.NAN;

import com.google.common.base.Predicate;

import org.sosy_lab.cpachecker.cpa.range.CompInteger;

import java.math.BigDecimal;


/**
 * A utility class for manipulating composite integers.
 */
public final class CompIntegers {

  private CompIntegers() {
  }

  private static final BigDecimal ALMOST_ZERO = new BigDecimal("0.000001");

  private static final Double ALMOST_ZERO_DBL = 0.000001;

  private static final Float ALMOST_ZERO_FLT = 0.000001f;

  /**
   * This delta value is used to represent exclusive borders.
   */
  public static final CompInteger ALMOST_ZERO_DELTA = new CompInteger(ALMOST_ZERO_DBL * 10);

  public static boolean isAlmostZero(BigDecimal decimal) {
    if (decimal.abs().compareTo(ALMOST_ZERO) < 0) {
      return true;
    }
    return false;
  }

  public static boolean isAlmostZero(Double value) {
    if (Math.abs(value) < ALMOST_ZERO_DBL) {
      return true;
    }
    return false;
  }

  public static boolean isAlmostZero(Float value) {
    if (Math.abs(value) < ALMOST_ZERO_FLT) {
      return true;
    }
    return false;
  }

  public static int signum(Double value) {
    if (value > ALMOST_ZERO_DBL) {
      return 1;
    } else if (value < -ALMOST_ZERO_DBL) {
      return -1;
    } else {
      return 0;
    }
  }

  /**
   * Get the fractional part of a decimal
   *
   * @param decimal a numerical value
   * @return fractional part, ranges in (-1, 1)
   */
  public static Double getFraction(BigDecimal decimal) {
    return reduce(decimal.remainder(BigDecimal.ONE));
  }

  public static Double reduce(BigDecimal decimal) {
    if (isAlmostZero(decimal)) {
      return 0.0;
    }
    return decimal.doubleValue();
  }

  public static final Predicate<CompInteger> notNaN = new Predicate<CompInteger>() {
    public boolean apply(CompInteger pCompInteger) {
      return (pCompInteger != null && pCompInteger.getStatus() != NAN);
    }
  };

  public static boolean overHalf(Double value) {
    Double absValue = Math.abs(value);
    if (absValue < 0.5) {
      return false;
    }
    return true;
  }

}
