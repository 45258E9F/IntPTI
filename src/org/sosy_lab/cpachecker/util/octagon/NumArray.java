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
package org.sosy_lab.cpachecker.util.octagon;

public class NumArray {

  private final long array;

  NumArray(long l) {
    array = l;
  }

  long getArray() {
    return array;
  }

  @Override
  public String toString() {
    // TODO
    return super.toString();
  }

  @Override
  public boolean equals(Object pObj) {
    if (!(pObj instanceof NumArray)) {
      return false;
    }
    NumArray otherArr = (NumArray) pObj;
    return this.array == otherArr.array;
  }

  @Override
  public int hashCode() {
    return (int) array;
  }
}