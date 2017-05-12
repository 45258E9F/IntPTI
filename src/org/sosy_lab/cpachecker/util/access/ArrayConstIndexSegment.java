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
package org.sosy_lab.cpachecker.util.access;

public class ArrayConstIndexSegment implements PathSegment {

  private final long index;

  public ArrayConstIndexSegment(long pIndex) {
    super();
    index = pIndex;
  }

  public long getIndex() {
    return index;
  }

  @Override
  public String getName() {
    return "[" + index + "]";
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    }
    if (that == null || !getClass().equals(that.getClass())) {
      return false;
    }
    ArrayConstIndexSegment other = (ArrayConstIndexSegment) that;
    if (index != other.index) {
      return false;
    }
    return true;
  }
}
