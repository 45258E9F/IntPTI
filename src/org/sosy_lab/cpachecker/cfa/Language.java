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
package org.sosy_lab.cpachecker.cfa;

/**
 * Enumeration for Supported Languages.
 */
public enum Language {
  C,
  JAVA,
  PHC,;

  @Override
  public String toString() {
    switch (this) {
      case C:
        return "C";
      case JAVA:
        return "Java";
      default:
        throw new AssertionError();
    }
  }
}
