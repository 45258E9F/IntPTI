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

/**
 * We don't record the slot offset
 */
public class ArrayAccessSegment implements PathSegment {

  public static ArrayAccessSegment INSTANCE = new ArrayAccessSegment();

  @Override
  public String getName() {
    return "[]";
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    }
    if (that == null || !getClass().equals(that.getClass())) {
      return false;
    }
    // by default, array access segments are all equal
    return true;
  }
}
