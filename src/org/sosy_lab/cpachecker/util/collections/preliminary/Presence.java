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
package org.sosy_lab.cpachecker.util.collections.preliminary;

/**
 * No information is stored in a Presence instance
 * It is just to distinguish from null
 */
public enum Presence {
  INSTANCE;

  @Override
  public String toString() {
    return "*";
  }
}
