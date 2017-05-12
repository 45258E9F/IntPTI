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
package org.sosy_lab.cpachecker.cpa.shape.merge.util;

import com.google.common.base.Objects;


/**
 * Joining two values (v1, v2) is sequence sensitive.
 * For example, (v1, v2) = v3 and (v2, v1) = v4 where v3 /= v4.
 * Therefore, the join pair is the unique identifier for value merging.
 */
public final class BasicValuePair {

  private Long left;
  private Long right;

  /**
   * The constructor
   *
   * @param l value from this state
   * @param r value from that state
   */
  private BasicValuePair(long l, long r) {
    left = l;
    right = r;
  }

  public static BasicValuePair of(long l, long r) {
    return new BasicValuePair(l, r);
  }

  public Long getLeft() {
    return left;
  }

  public Long getRight() {
    return right;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(left, right);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof BasicValuePair)) {
      return false;
    }
    BasicValuePair other = (BasicValuePair) obj;
    return left.equals(other.left) && right.equals(other.right);
  }

  @Override
  public String toString() {
    return "[" + left + "," + right + "]";
  }

}
