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
package org.sosy_lab.cpachecker.cpa.range;

import com.google.common.base.Objects;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import org.sosy_lab.cpachecker.cpa.range.util.CompIntegers;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import scala.actors.threadpool.Arrays;

public final class Range {

  private final CompInteger low;
  private final CompInteger high;

  public Range(long value) {
    this.low = new CompInteger(value);
    this.high = new CompInteger(value);
  }

  public Range(CompInteger value) {
    this.low = value;
    this.high = value;
  }

  public Range(long low, long high) {
    if (low <= high) {
      this.low = new CompInteger(low);
      this.high = new CompInteger(high);
    } else {
      this.low = new CompInteger(high);
      this.high = new CompInteger(low);
    }
  }

  public Range(double low, double high) {
    if (low <= high) {
      this.low = new CompInteger(low);
      this.high = new CompInteger(high);
    } else {
      this.low = new CompInteger(high);
      this.high = new CompInteger(low);
    }
  }

  public Range(CompInteger low, CompInteger high) {
    if ((low == null) != (high == null)) {
      throw new IllegalStateException("Range should not partially NULL");
    }
    if (low == null) {
      this.low = null;
      this.high = null;
      return;
    }
    if (low.equals(CompInteger.NaN) || high.equals(CompInteger.NaN)) {
      // NaN is an undetermined value
      this.low = CompInteger.NEGATIVE_INF;
      this.high = CompInteger.POSITIVE_INF;
    } else {
      if (low.compareTo(high) <= 0) {
        this.low = low;
        this.high = high;
      } else {
        this.low = high;
        this.high = low;
      }
    }
  }

  public static Range singletonRange(long value) {
    return new Range(value);
  }

  public static Range upperBoundedRange(CompInteger bound) {
    return new Range(CompInteger.NEGATIVE_INF, bound);
  }

  public static Range lowerBoundedRange(CompInteger bound) {
    return new Range(bound, CompInteger.POSITIVE_INF);
  }

  public CompInteger getLow() {
    return this.low;
  }

  public CompInteger getHigh() {
    return this.high;
  }

  public CompInteger getLength() {
    if (isEmpty()) {
      throw new IllegalStateException("Empty set does not support this operation");
    }
    return this.high.subtract(this.low);
  }

  /**
   * Different from the {@link Range#getLength()}, this method returns number of integers in this
   * range.
   *
   * @return number of integers
   */
  public CompInteger numOfIntegers() {
    if (isEmpty()) {
      return CompInteger.ZERO;
    }
    return this.high.floor().subtract(this.low.ceil()).add(CompInteger.ONE);
  }

  public static final Range ZERO = new Range(0L);
  public static final Range ONE = new Range(1L);
  public static final Range BOOL = new Range(0L, 1L);
  /**
   * {@link Range#EMPTY} denotes empty set
   */
  public static final Range EMPTY = new Range(null, null);
  public static final Range UNBOUND = new Range(CompInteger.NEGATIVE_INF, CompInteger.POSITIVE_INF);
  public static final Range NONNEGATIVE = new Range(CompInteger.ZERO, CompInteger.POSITIVE_INF);

  public boolean isSingletonRange() {
    if (isEmpty()) {
      return false;
    }
    if (low.equals(high)) {
      return true;
    }
    return false;
  }

  @Override
  public boolean equals(Object other) {
    if (other != null && getClass().equals(other.getClass())) {
      Range that = (Range) other;
      if (isEmpty() && that.isEmpty()) {
        return true;
      } else if (isEmpty() != that.isEmpty()) {
        return false;
      }
      return low.equals(that.low) && high.equals(that.high);
    } else {
      return false;
    }
  }

  public boolean isEmpty() {
    return low == null && high == null;
  }

  public boolean isUnbound() {
    return !isEmpty() && low.equals(CompInteger.NEGATIVE_INF) && high.equals(CompInteger
        .POSITIVE_INF);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(low, high);
  }

  public Range union(Range that) {
    if (isEmpty()) {
      // correction: empty set is identity in interval union operation
      return that;
    } else if (that.isEmpty()) {
      return this;
    } else if (low.compareTo(that.low) >= 0 && high.compareTo(that.high) <=
        0) {
      return that;
    } else {
      return new Range(low.min(that.low), high.max(that.high));
    }
  }

  /**
   * This function checks if two ranges overlap
   *
   * @param that the second operand
   * @return whether two ranges overlap
   */
  public boolean intersects(Range that) {
    if (isEmpty() || that.isEmpty()) {
      return false;
    }
    return (low.compareTo(that.low) >= 0 && low.compareTo(that.high) <= 0) || (high.compareTo
        (that.low) >= 0 && high.compareTo(that.high) <= 0) || (low.compareTo(that.low) <= 0 &&
        high.compareTo(that.high) >= 0);
  }

  public Range intersect(Range that) {
    if (this.intersects(that)) {
      return new Range(low.max(that.low), high.min(that.high));
    } else {
      return EMPTY;
    }
  }

  /**
   * Subtraction of intervals. For example, [4,10] \ [6,12] = [4,6]
   * Note: A special case: [4,10] \ [6,8] would make the range split
   */
  public Range complement(Range that) {
    Range overlap = this.intersect(that);
    if (overlap.isEmpty()) {
      return this;
    } else if (overlap.equals(this)) {
      return Range.EMPTY;
    } else if (overlap.getLow().equals(low)) {
      return new Range(overlap.getHigh().add(CompInteger.ONE), high);
    } else if (overlap.getHigh().equals(high)) {
      return new Range(low, overlap.getLow().subtract(CompInteger.ONE));
    } else {
      // though imprecise, this makes the analysis sound
      return this;
    }
  }

  public boolean isGreaterThan(Range that) {
    return !isEmpty() && !that.isEmpty() && low.compareTo(that.high) > 0;
  }

  public boolean isGreaterOrEqualThan(Range that) {
    return !isEmpty() && !that.isEmpty() && low.compareTo(that.high) >= 0;
  }

  public boolean mayBeGreaterThan(Range that) {
    return that.isEmpty() || (!isEmpty() && !that.isEmpty() && high.compareTo(that.low) > 0);
  }

  public boolean contains(Range that) {
    return that.isEmpty() || (!isEmpty() && !that.isEmpty() && low.compareTo(that.low) <= 0 &&
        high.compareTo(that.high) >= 0);
  }

  public boolean in(long value) {
    return !isEmpty() && low.compareTo(BigInteger.valueOf(value)) <= 0 && high.compareTo
        (BigInteger.valueOf(value)) >= 0;
  }

  public boolean in(CompInteger value) {
    return !isEmpty() && low.compareTo(value) <= 0 && high.compareTo(value) >= 0;
  }

  public Range modulo(Range that) {
    if (this.isEmpty() || that.isEmpty()) {
      return EMPTY;
    }

    // The sign of {@code a % b} is consistent with that of {@code a}
    CompInteger lower, upper;
    if (that.contains(ZERO)) {
      lower = CompInteger.ONE;
      upper = that.low.abs().max(that.high.abs());
    } else {
      lower = that.low.abs();
      upper = that.high.abs();
    }
    Range other = new Range(lower, upper);

    CompInteger newLow, newHigh;

    CompInteger top;
    if (low.signum() >= 0) {
      top = high;
    } else {
      top = low.abs().max(high);
    }

    newHigh = top.min(other.high.subtract(CompInteger.ONE));

    if (low.signum() >= 0) {
      if (low.signum() == 0 || high.compareTo(other.low) >= 0) {
        newLow = CompInteger.ZERO;
      } else {
        newLow = low;
      }
    } else {
      newLow = low.max(CompInteger.ONE.subtract(other.high));
    }

    return new Range(newLow, newHigh);
  }

  public Range limitUpperBoundBy(Range that) {
    if (isEmpty() || that.isEmpty() || low.compareTo(that.high) > 0) {
      return EMPTY;
    } else {
      return new Range(low, high.min(that.high));
    }
  }

  public Range limitLowerBoundBy(Range that) {
    if (isEmpty() || that.isEmpty() || high.compareTo(that.low) < 0) {
      return EMPTY;
    } else {
      return new Range(low.max(that.low), high);
    }
  }

  public Range plus(Range that) {
    if (isEmpty() || that.isEmpty()) {
      return EMPTY;
    }
    return new Range(low.add(that.low), high.add(that.high));
  }

  public Range plus(Long offset) {
    return plus(new Range(offset));
  }

  public Range minus(Range that) {
    return plus(that.negate());
  }

  public Range minus(Long offset) {
    return plus(-offset);
  }

  public Range negate() {
    if (isEmpty()) {
      return EMPTY;
    }
    return new Range(high.negate(), low.negate());
  }

  private CompInteger[] removeNaN(CompInteger[] valueList) {
    return FluentIterable.of(valueList).filter(CompIntegers.notNaN).toArray(CompInteger.class);
  }

  public Range times(Range that) {
    if (isEmpty() || that.isEmpty()) {
      return EMPTY;
    }
    CompInteger[] values = {low.multiply(that.low), low.multiply(that.high), high.multiply(that
        .low), high.multiply(that.high)};
    values = removeNaN(values);
    CompInteger min = (CompInteger) Collections.min(Arrays.asList(values));
    CompInteger max = (CompInteger) Collections.max(Arrays.asList(values));
    return new Range(min, max);
  }

  public Range divide(Range that) {
    if (isEmpty() || that.isEmpty()) {
      return EMPTY;
    }
    if (that.contains(ZERO)) {
      CompInteger upper = low.abs().max(high.abs());
      return new Range(upper.negate(), upper);
    } else {
      CompInteger[] values = {low.divide(that.low), low.divide(that.high), high.divide(that.low)
          , high.divide(that.high)};
      values = removeNaN(values);
      CompInteger min = (CompInteger) Collections.min(Arrays.asList(values));
      CompInteger max = (CompInteger) Collections.max(Arrays.asList(values));
      return new Range(min, max);
    }
  }

  /**
   * Discard partial fractional parts.
   *
   * @return Truncated range
   */
  public Range compress() {
    if (isEmpty()) {
      return EMPTY;
    }
    CompInteger lower, upper;
    lower = low.ceil();
    upper = high.floor();
    if (lower.compareTo(upper) > 0) {
      return EMPTY;
    }
    return new Range(lower, upper);
  }

  /**
   * Bitwise left shift operation.
   * Note: If the offset range contains non-zero fractional part (e.g. [4.7, 6.8]), then we just
   * truncate these partial fractional parts (e.g. the result is [5, 6]).
   *
   * @param offset the range of offset
   * @return the range of bitwise left shift operation
   */
  public Range shiftLeft(Range offset) {
    offset = offset.compress();
    if (isEmpty() || offset.isEmpty()) {
      return EMPTY;
    }

    CompInteger newLow, newHigh;
    CompInteger lTaintLow, lTaintHigh;
    Integer rTaintLow, rTaintHigh;

    if (ZERO.mayBeGreaterThan(offset)) {
      if (offset.high.signum() < 0) {
        return EMPTY;
      } else {
        rTaintLow = 0;
        rTaintHigh = offset.high.intValue();
      }
    } else {
      rTaintLow = offset.low.intValue();
      rTaintHigh = offset.high.intValue();
    }

    if (ZERO.mayBeGreaterThan(this)) {
      if (high.signum() < 0) {
        // we do not permit negative values in the left operand
        return EMPTY;
      } else {
        lTaintLow = CompInteger.ZERO;
        lTaintHigh = high;
      }
    } else {
      lTaintLow = low;
      lTaintHigh = high;
    }

    if (rTaintLow == null) {
      // too large to fit INT type
      if (lTaintLow.signum() == 0) {
        newLow = CompInteger.ZERO;
      } else {
        newLow = CompInteger.POSITIVE_INF;
      }
    } else {
      newLow = lTaintLow.shiftLeft(rTaintLow);
    }

    if (rTaintHigh == null) {
      if (lTaintHigh.signum() == 0) {
        newHigh = CompInteger.ZERO;
      } else {
        newHigh = CompInteger.POSITIVE_INF;
      }
    } else {
      newHigh = lTaintHigh.shiftLeft(rTaintHigh);
    }

    return new Range(newLow, newHigh);
  }

  public Range shiftRight(Range offset) {
    offset = offset.compress();
    if (isEmpty() || offset.isEmpty()) {
      return EMPTY;
    }
    CompInteger newLow, newHigh;
    CompInteger lTaintLow, lTaintHigh;
    Integer rTaintLow, rTaintHigh;

    if (ZERO.mayBeGreaterThan(offset)) {
      if (offset.high.signum() < 0) {
        return EMPTY;
      } else {
        rTaintLow = 0;
        rTaintHigh = offset.high.intValue();
      }
    } else {
      rTaintLow = offset.low.intValue();
      rTaintHigh = offset.high.intValue();
    }

    if (ZERO.mayBeGreaterThan(this)) {
      if (high.signum() < 0) {
        return EMPTY;
      } else {
        lTaintLow = CompInteger.ZERO;
        lTaintHigh = high;
      }
    } else {
      lTaintLow = low;
      lTaintHigh = high;
    }

    if (rTaintHigh == null) {
      newLow = CompInteger.ZERO;
    } else {
      newLow = lTaintLow.shiftRight(rTaintHigh);
    }

    if (rTaintLow == null) {
      newHigh = CompInteger.ZERO;
    } else {
      newHigh = lTaintHigh.shiftRight(rTaintLow);
    }

    return new Range(newLow, newHigh);
  }

  /**
   * To support numerical library functions, we selectively implement some common arithmetic
   * operations in range analysis
   */

  public static final Range TRIGONOMETRIC_VALUES = new Range(-1, 1);
  /**
   * An approximation of [-0.5\pi, +0.5\pi]
   */
  public static final Range TRIGONOMETRIC_SYM_DOMAIN = new Range(-Math.PI / 2, Math.PI / 2);
  /**
   * An approximation of [0, \pi]
   */
  public static final Range TRIGONOMETRIC_UNSYM_DOMAIN = new Range(CompInteger.ZERO, new
      CompInteger(Math.PI));

  /**
   * arcsin function accepts x \in [-1,1] and returns a value in [-0.5\pi, 0.5\pi]
   *
   * @return arcsin(x)
   */
  public Range asin() {
    if (this.isEmpty()) {
      return Range.EMPTY;
    }
    CompInteger lower, upper;
    if (low.compareTo(TRIGONOMETRIC_VALUES.low) <= 0) {
      lower = TRIGONOMETRIC_SYM_DOMAIN.low;
    } else if (low.compareTo(TRIGONOMETRIC_VALUES.high) >= 0) {
      lower = TRIGONOMETRIC_SYM_DOMAIN.high;
    } else {
      Double lowDouble = low.doubleValue();
      lower = new CompInteger(lowDouble);
    }
    if (high.compareTo(TRIGONOMETRIC_VALUES.high) >= 0) {
      upper = TRIGONOMETRIC_SYM_DOMAIN.high;
    } else if (high.compareTo(TRIGONOMETRIC_VALUES.low) <= 0) {
      upper = TRIGONOMETRIC_SYM_DOMAIN.low;
    } else {
      Double highDouble = high.doubleValue();
      upper = new CompInteger(highDouble);
    }
    return new Range(lower, upper);
  }

  public Range acos() {
    if (this.isEmpty()) {
      return Range.EMPTY;
    }
    CompInteger lower, upper;
    if (low.compareTo(TRIGONOMETRIC_VALUES.low) <= 0) {
      upper = TRIGONOMETRIC_UNSYM_DOMAIN.high;
    } else if (low.compareTo(TRIGONOMETRIC_VALUES.high) >= 0) {
      upper = TRIGONOMETRIC_UNSYM_DOMAIN.low;
    } else {
      Double highDouble = low.doubleValue();
      upper = new CompInteger(highDouble);
    }
    if (high.compareTo(TRIGONOMETRIC_VALUES.high) >= 0) {
      lower = TRIGONOMETRIC_UNSYM_DOMAIN.low;
    } else if (high.compareTo(TRIGONOMETRIC_VALUES.low) <= 0) {
      lower = TRIGONOMETRIC_UNSYM_DOMAIN.high;
    } else {
      Double lowDouble = high.doubleValue();
      lower = new CompInteger(lowDouble);
    }
    return new Range(lower, upper);
  }

  public Range atan() {
    if (this.isEmpty()) {
      return Range.EMPTY;
    }
    CompInteger lower, upper;
    Double lowDouble = low.doubleValue();
    lower = new CompInteger(lowDouble);
    Double highDouble = high.doubleValue();
    upper = new CompInteger(highDouble);
    return new Range(lower, upper);
  }

  /**
   * arctan(y, x) = arctan(y/x)
   * y.atan2(x) = arctan(y, x)
   *
   * @param that another range (x)
   * @return arctan(y/x)
   */
  public Range atan2(Range that) {
    if (this.isEmpty() || that.isEmpty()) {
      return EMPTY;
    }
    Range divideRange = this.divide(that);
    return divideRange.atan();
  }

  /**
   * Compute cos(x). Cosine function is periodic. We do not analyze this function in a
   * refined manner since it can be very expensive.
   *
   * @return cos(x)
   */
  public Range cos() {
    if (this.isEmpty()) {
      return EMPTY;
    }
    return TRIGONOMETRIC_VALUES;
  }

  public Range sin() {
    if (this.isEmpty()) {
      return EMPTY;
    }
    return TRIGONOMETRIC_VALUES;
  }

  public Range tan() {
    if (this.isEmpty()) {
      return EMPTY;
    }
    if (getLength().compareTo(CompInteger.PI_LAPPROX) >= 0) {
      return Range.UNBOUND;
    }
    // check if the domain contains a singular point
    Double domainLow = low.doubleValue();
    Double domainHigh = high.doubleValue();
    // consider the coordinate of singular: (2k + 1) / 2 * \pi
    Double lowPeriod = (domainLow * 2 / Math.PI - 1) / 2;
    Double highPeriod = (domainHigh * 2 / Math.PI - 1) / 2;
    if (lowPeriod.longValue() == highPeriod.longValue()) {
      // they are in the same period
      Double rangeLow = Math.tan(domainLow);
      Double rangeHigh = Math.tan(domainHigh);
      return new Range(rangeLow, rangeHigh);
    } else {
      return Range.UNBOUND;
    }
  }

  public Range exp() {
    if (this.isEmpty()) {
      return EMPTY;
    }
    Double domainLow = low.doubleValue();
    Double domainHigh = high.doubleValue();
    Double rangeLow = Math.exp(domainLow);
    Double rangeHigh = Math.exp(domainHigh);
    return new Range(rangeLow, rangeHigh);
  }

  public Range exp2() {
    if (this.isEmpty()) {
      return EMPTY;
    }
    Double domainLow = low.doubleValue();
    Double domainHigh = high.doubleValue();
    Double rangeLow = Math.pow(2, domainLow);
    Double rangeHigh = Math.pow(2, domainHigh);
    return new Range(rangeLow, rangeHigh);
  }

  public Range expm1() {
    if (this.isEmpty()) {
      return EMPTY;
    }
    Double domainLow = low.doubleValue();
    Double domainHigh = high.doubleValue();
    Double rangeLow = Math.expm1(domainLow);
    Double rangeHigh = Math.expm1(domainHigh);
    return new Range(rangeLow, rangeHigh);
  }

  public Range log() {
    if (this.isEmpty()) {
      return EMPTY;
    }
    // the domain of log() is requires to be positive
    // when the argument ranges in [0,1], the value ranges in [-\infty, 0]
    CompInteger lower, upper;
    if (low.compareTo(CompInteger.ZERO) <= 0) {
      lower = CompInteger.NEGATIVE_INF;
    } else {
      Double domainLow = low.doubleValue();
      Double rangeLow = Math.log(domainLow);
      lower = new CompInteger(rangeLow);
    }
    if (high.compareTo(CompInteger.ZERO) <= 0) {
      upper = CompInteger.NEGATIVE_INF;
    } else {
      Double domainHigh = high.doubleValue();
      Double rangeHigh = Math.log(domainHigh);
      upper = new CompInteger(rangeHigh);
    }
    return new Range(lower, upper);
  }

  public Range log10() {
    if (this.isEmpty()) {
      return EMPTY;
    }
    CompInteger lower, upper;
    if (low.compareTo(CompInteger.ZERO) <= 0) {
      lower = CompInteger.NEGATIVE_INF;
    } else {
      Double domainLow = low.doubleValue();
      Double rangeLow = Math.log10(domainLow);
      lower = new CompInteger(rangeLow);
    }
    if (high.compareTo(CompInteger.ZERO) <= 0) {
      upper = CompInteger.NEGATIVE_INF;
    } else {
      Double domainHigh = high.doubleValue();
      Double rangeHigh = Math.log10(domainHigh);
      upper = new CompInteger(rangeHigh);
    }
    return new Range(lower, upper);
  }

  public Range log1p() {
    if (this.isEmpty()) {
      return EMPTY;
    }
    CompInteger lower, upper;
    if (low.compareTo(CompInteger.ZERO) <= 0) {
      lower = CompInteger.NEGATIVE_INF;
    } else {
      Double domainLow = low.doubleValue();
      Double rangeLow = Math.log1p(domainLow);
      lower = new CompInteger(rangeLow);
    }
    if (high.compareTo(CompInteger.ZERO) <= 0) {
      upper = CompInteger.NEGATIVE_INF;
    } else {
      Double domainHigh = high.doubleValue();
      Double rangeHigh = Math.log1p(domainHigh);
      upper = new CompInteger(rangeHigh);
    }
    return new Range(lower, upper);
  }

  public Range log2() {
    if (this.isEmpty()) {
      return EMPTY;
    }
    CompInteger lower, upper;
    if (low.compareTo(CompInteger.ZERO) <= 0) {
      lower = CompInteger.NEGATIVE_INF;
    } else {
      Double domainLow = low.doubleValue();
      Double rangeLow = Math.log(domainLow) / Math.log(2);
      lower = new CompInteger(rangeLow);
    }
    if (high.compareTo(CompInteger.ZERO) <= 0) {
      upper = CompInteger.NEGATIVE_INF;
    } else {
      Double domainHigh = high.doubleValue();
      Double rangeHigh = Math.log(domainHigh) / Math.log(2);
      upper = new CompInteger(rangeHigh);
    }
    return new Range(lower, upper);
  }

  public Range cbrt() {
    if (this.isEmpty()) {
      return EMPTY;
    }
    Double domainLow = low.doubleValue();
    Double domainHigh = high.doubleValue();
    Double rangeLow = Math.cbrt(domainLow);
    Double rangeHigh = Math.cbrt(domainHigh);
    return new Range(rangeLow, rangeHigh);
  }

  public Range sqrt() {
    if (this.isEmpty()) {
      return EMPTY;
    }
    CompInteger lower, upper;
    if (low.compareTo(CompInteger.ZERO) <= 0) {
      lower = CompInteger.ZERO;
    } else {
      Double domainLow = low.doubleValue();
      Double rangeLow = Math.sqrt(domainLow);
      lower = new CompInteger(rangeLow);
    }
    if (high.compareTo(CompInteger.ZERO) <= 0) {
      upper = CompInteger.ZERO;
    } else {
      Double domainHigh = high.doubleValue();
      Double rangeHigh = Math.sqrt(domainHigh);
      upper = new CompInteger(rangeHigh);
    }
    return new Range(lower, upper);
  }

  public Range abs() {
    if (this.isEmpty()) {
      return EMPTY;
    }
    if (low.signum() <= 0 && high.signum() >= 0) {
      return new Range(CompInteger.ZERO, low.abs().max(high.abs()));
    }
    CompInteger lower = low.abs();
    CompInteger upper = high.abs();
    if (lower.compareTo(upper) <= 0) {
      return new Range(lower, upper);
    } else {
      return new Range(upper, lower);
    }
  }

  /**
   * this: x
   * that: y
   * x.hypot(y) = \sqrt(x^2 + y^2)
   *
   * @param that y
   * @return Euclidean distance of (x,y) and (0,0)
   */
  public Range hypot(Range that) {
    if (this.isEmpty() || that.isEmpty()) {
      return EMPTY;
    }
    // first, check if x (or y) contains 0 in its range
    Double domainXLow, domainXHigh;
    Double domainYLow, domainYHigh;
    domainXLow = this.low.doubleValue();
    domainXHigh = this.high.doubleValue();
    if (domainXLow <= 0 && domainXHigh >= 0) {
      domainXHigh = Math.max(Math.abs(domainXLow), Math.abs(domainXHigh));
      domainXLow = 0.0;
    } else {
      domainXLow = Math.abs(domainXLow);
      domainXHigh = Math.abs(domainXHigh);
      if (domainXLow > domainXHigh) {
        // two values should be swapped
        Double temp = domainXLow;
        domainXLow = domainXHigh;
        domainXHigh = temp;
      }
    }
    domainYLow = that.low.doubleValue();
    domainYHigh = that.high.doubleValue();
    if (domainYLow <= 0 && domainYHigh >= 0) {
      domainYHigh = Math.max(Math.abs(domainYLow), Math.abs(domainYHigh));
      domainYLow = 0.0;
    } else {
      domainYLow = Math.abs(domainYLow);
      domainYHigh = Math.abs(domainYHigh);
      if (domainYLow > domainYHigh) {
        // two values should be swapped
        Double temp = domainYLow;
        domainYLow = domainYHigh;
        domainYHigh = temp;
      }
    }
    // finally, we compute bounds of this function
    return new Range(Math.hypot(domainXLow, domainYLow), Math.hypot(domainXHigh,
        domainYHigh));
  }

  /**
   * Power function. x.pow(y) computes x^y.
   * The value of x should be non-negative for avoiding exception.
   * If x equals 0, y should be positive, otherwise errors would occur
   *
   * @param that y
   * @return x.pow(y)
   */
  public Range pow(Range that) {
    if (this.isEmpty() || that.isEmpty()) {
      return EMPTY;
    }
    // x = 1 is a special border. In this border, y keeps to be 1.
    // if x < 1, x^y monotonically decreases with respect to y;
    // if x > 1, x^y monotonically increases (...)
    // ** IN A WORD, THE BORDER VALUES ARE DERIVED BY VERTEX OF x--y
    if (this.low.compareTo(CompInteger.ZERO) <= 0) {
      // in this case, we truncate negative values
      if (that.low.compareTo(CompInteger.ZERO) <= 0) {
        // now the following case is possible: x == 0 && y <= 0
        CompInteger lower, upper;
        // according to 3D plot generated by Wolfram Alpha
        upper = CompInteger.POSITIVE_INF;
        if (that.high.compareTo(CompInteger.ZERO) <= 0) {
          lower = CompInteger.POSITIVE_INF;
        } else {
          lower = CompInteger.ZERO;
        }
        return new Range(lower, upper);
      } else {
        CompInteger lower, upper;
        lower = CompInteger.ZERO;
        Double domainXHigh = this.high.doubleValue();
        Double domainYLow = that.low.doubleValue();
        Double domainYHigh = that.high.doubleValue();
        List<Double> powers = Lists.newArrayList(Math.pow(domainXHigh, domainYLow), Math.pow
            (domainXHigh, domainYHigh));
        Double upperDouble = Collections.max(powers);
        upper = new CompInteger(Math.ceil(upperDouble));
        return new Range(lower, upper);
      }
    } else {
      // directly compute four values for maximum and minimum ones
      Double domainXLow = this.low.doubleValue();
      Double domainXHigh = this.high.doubleValue();
      Double domainYLow = that.low.doubleValue();
      Double domainYHigh = that.high.doubleValue();
      List<Double> powers = Lists.newArrayList(Math.pow(domainXLow, domainYLow), Math.pow
          (domainXHigh, domainYLow), Math.pow(domainXLow, domainYHigh), Math.pow(domainXHigh,
          domainYHigh));
      Double lowerDouble = Collections.min(powers);
      Double upperDouble = Collections.max(powers);
      return new Range(lowerDouble, upperDouble);
    }
  }

  public Range ceil() {
    if (isEmpty()) {
      return EMPTY;
    }
    return new Range(low.ceil(), high.ceil());
  }

  public Range floor() {
    if (isEmpty()) {
      return EMPTY;
    }
    return new Range(low.floor(), high.floor());
  }

  public Range round() {
    if (isEmpty()) {
      return EMPTY;
    }
    return new Range(low.round(), high.round());
  }

  public Range trunc() {
    if (isEmpty()) {
      return EMPTY;
    }
    return new Range(low.trunc(), high.trunc());
  }

  public Range fdim(Range that) {
    if (isEmpty() || that.isEmpty()) {
      return EMPTY;
    }
    CompInteger lower, upper;
    List<CompInteger> distances = Lists.newArrayList(low.subtract(that.low).abs(), low.subtract
        (that.high).abs(), high.subtract(that.low).abs(), high.subtract(that.high).abs());
    if (this.intersects(that)) {
      lower = CompInteger.ZERO;
    } else {
      lower = Collections.min(distances);
    }
    upper = Collections.max(distances);
    return new Range(lower, upper);
  }

  public Range max(Range that) {
    if (isEmpty() || that.isEmpty()) {
      return EMPTY;
    }
    CompInteger lower, upper;
    lower = low.max(that.low);
    upper = high.max(that.high);
    return new Range(lower, upper);
  }

  public Range min(Range that) {
    if (isEmpty() || that.isEmpty()) {
      return EMPTY;
    }
    CompInteger lower, upper;
    lower = low.min(that.low);
    upper = high.min(that.high);
    return new Range(lower, upper);
  }

  /**
   * x.fma(y, z) = (x * y) + z.
   *
   * @param op1 y
   * @param op2 z
   * @return x.fma(y, z)
   */
  public Range fma(Range op1, Range op2) {
    if (isEmpty() || op1.isEmpty() || op2.isEmpty()) {
      return EMPTY;
    }
    return this.times(op1).plus(op2);
  }

  @Override
  public String toString() {
    return "[" + (low == null ? "" : low.toString()) + "; " + (high == null ? "" : high.toString
        ()) + "]";
  }

}
