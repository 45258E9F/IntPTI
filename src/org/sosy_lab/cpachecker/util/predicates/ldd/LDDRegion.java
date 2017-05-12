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
package org.sosy_lab.cpachecker.util.predicates.ldd;

import org.sosy_lab.cpachecker.util.predicates.regions.Region;


public class LDDRegion implements Region {

  private final LDD ldd;

  public LDDRegion(LDD ldd) {
    this.ldd = ldd;
  }

  LDD getLDD() {
    return this.ldd;
  }

  @Override
  public boolean isTrue() {
    return this.ldd.isOne();
  }

  @Override
  public boolean isFalse() {
    return this.ldd.isZero();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof LDDRegion) {
      return ldd.equals(((LDDRegion) o).ldd);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return ldd.hashCode();
  }

}
