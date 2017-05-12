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
package org.sosy_lab.cpachecker.util.predicates.bdd;

import com.google.common.primitives.Longs;

import org.sosy_lab.cpachecker.util.predicates.regions.Region;

import jsylvan.JSylvan;

/**
 * Regions represented using Sylvan BDDs.
 */
public class SylvanBDDRegion implements Region {

  private final long bddRepr;

  SylvanBDDRegion(long pBDD) {
    bddRepr = pBDD;
  }

  @Override
  public boolean isTrue() {
    return bddRepr == JSylvan.getTrue();
  }

  @Override
  public boolean isFalse() {
    return bddRepr == JSylvan.getFalse();
  }

  long getBDD() {
    return bddRepr;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof SylvanBDDRegion) {
      return bddRepr == ((SylvanBDDRegion) o).bddRepr;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Longs.hashCode(bddRepr);
  }

  @Override
  public String toString() {
    if (isTrue()) {
      return "true";
    } else if (isFalse()) {
      return "false";
    } else {
      return bddRepr + "";
    }
  }
}
