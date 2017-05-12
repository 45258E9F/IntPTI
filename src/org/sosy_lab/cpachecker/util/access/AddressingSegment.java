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
package org.sosy_lab.cpachecker.util.access;

/**
 * This segment represents the addressing operation (&). In principle, this segment should only
 * be at the end of the whole access path. Developer is responsible to make this property hold.
 */
public class AddressingSegment implements PathSegment {

  public static AddressingSegment INSTANCE = new AddressingSegment();

  @Override
  public String getName() {
    return "&";
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    }
    if (that == null || !getClass().equals(that.getClass())) {
      return false;
    }
    // by default, two addressing segments must equal
    return true;
  }
}
