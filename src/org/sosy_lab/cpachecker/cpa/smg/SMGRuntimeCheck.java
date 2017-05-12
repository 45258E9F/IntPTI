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
package org.sosy_lab.cpachecker.cpa.smg;

enum SMGRuntimeCheck {
  FORCED(-1),
  NONE(0),
  HALF(1),
  FULL(2);

  private final int id;

  SMGRuntimeCheck(int pId) {
    id = pId;
  }

  public int getValue() {
    return id;
  }

  public boolean isFinerOrEqualThan(SMGRuntimeCheck other) {
    return id >= other.id;
  }
}