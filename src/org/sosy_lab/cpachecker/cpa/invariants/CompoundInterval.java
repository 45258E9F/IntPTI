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
package org.sosy_lab.cpachecker.cpa.invariants;

import java.math.BigInteger;
import java.util.List;

/**
 * Instances of this class represent compound states of intervals.
 */
public interface CompoundInterval {

  boolean isSingleton();

  boolean hasLowerBound();

  boolean hasUpperBound();

  BigInteger getLowerBound();

  BigInteger getUpperBound();

  BigInteger getValue();

  boolean isDefinitelyFalse();

  boolean isDefinitelyTrue();

  boolean isBottom();

  boolean contains(BigInteger pBigInteger);

  CompoundInterval extendToMinValue();

  CompoundInterval extendToMaxValue();

  CompoundInterval invert();

  CompoundInterval span();

  /**
   * Checks if this compound state contains every possible value.
   *
   * @return {@code true} if this state contains every possible value, {@code false} otherwise.
   */
  boolean containsAllPossibleValues();

  boolean containsNegative();

  boolean containsPositive();

  CompoundInterval signum();

  List<? extends CompoundInterval> splitIntoIntervals();

  List<SimpleInterval> getIntervals();

}
