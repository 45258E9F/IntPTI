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
package org.sosy_lab.cpachecker.cpa.range;

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.util.access.ArrayConstIndexSegment;
import org.sosy_lab.cpachecker.util.access.PathSegment;

/**
 * This segment tracks uncertain index value in array subscript,
 * which is necessary for sound array element analysis
 */
public class ArrayUncertainIndexSegment implements PathSegment {

  private final Range indexRange;

  public ArrayUncertainIndexSegment(Range pRange) {
    super();
    // if the input range is [4.3, 6.7], then {5,6} are valid index values
    indexRange = pRange.compress();
  }

  public boolean isSingletonIndexRange() {
    return indexRange.isSingletonRange();
  }

  public boolean isEmptyIndexRange() {
    return indexRange.isEmpty();
  }

  public Optional<ArrayConstIndexSegment> convertToConstIndexSegment() {
    if (isSingletonIndexRange()) {
      Long longIndex = indexRange.getLow().longValue();
      if (longIndex != null) {
        return Optional.of(new ArrayConstIndexSegment(longIndex));
      }
    }
    return Optional.absent();
  }

  public Range getIndexRange() {
    return indexRange;
  }

  @Override
  public String getName() {
    return "[" + indexRange.toString() + "]";
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    }
    if (that == null || !getClass().equals(that.getClass())) {
      return false;
    }
    ArrayUncertainIndexSegment other = (ArrayUncertainIndexSegment) that;
    if (!indexRange.equals(other.indexRange)) {
      return false;
    }
    return true;
  }
}
