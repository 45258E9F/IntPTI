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
package org.sosy_lab.cpachecker.cpa.pointer2.util;

import org.sosy_lab.cpachecker.util.states.MemoryLocation;


public enum LocationSetTop implements LocationSet {

  INSTANCE;

  @Override
  public boolean mayPointTo(MemoryLocation pTarget) {
    return true;
  }

  @Override
  public LocationSet addElement(MemoryLocation pTarget) {
    return this;
  }

  /**
   * This operation does not remove the given target from the set, thus
   * the resulting set is only an over-approximation of the represented
   * conceptual set. For a precise representation, it is necessary to
   * know the complete set of potential targets and remove the given
   * target from it.
   *
   * @param pTarget the target to remove.
   * @return the same unchanged object.
   */
  @Override
  public LocationSet removeElement(MemoryLocation pTarget) {
    return this;
  }

  @Override
  public LocationSet addElements(Iterable<MemoryLocation> pTargets) {
    return this;
  }

  @Override
  public boolean isBot() {
    return false;
  }

  @Override
  public boolean isTop() {
    return true;
  }

  @Override
  public LocationSet addElements(LocationSet pElements) {
    return this;
  }

  @Override
  public boolean containsAll(LocationSet pElements) {
    return true;
  }

  @Override
  public String toString() {
    return Character.toString('\u22A4');
  }

}
