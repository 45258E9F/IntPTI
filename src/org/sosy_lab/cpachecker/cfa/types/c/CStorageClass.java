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
package org.sosy_lab.cpachecker.cfa.types.c;

/**
 * Enum for the possible storage classes of C declarations.
 * REGISTER is missing because it is semantically equal to AUTO.
 */
public enum CStorageClass {

  AUTO,
  STATIC,
  EXTERN,
  TYPEDEF,;

  public String toASTString() {
    if (equals(AUTO)) {
      return "";
    }
    return name().toLowerCase() + " ";
  }
}
