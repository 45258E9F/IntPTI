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

/**
 * Target Specifier for specifying target of pointer.
 */
public enum SMGTargetSpecifier {
  REGION,
  FIRST,
  LAST,
  ALL,
  UNKNOWN;

  @Override
  public String toString() {
    switch (this) {
      case REGION:
        return "reg";
      case FIRST:
        return "fst";
      case LAST:
        return "lst";
      case ALL:
        return "all";
      case UNKNOWN:
        return "unknown";
      default:
        throw new AssertionError();
    }
  }
}
