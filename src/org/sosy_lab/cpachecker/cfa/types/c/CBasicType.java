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

public enum CBasicType {

  UNSPECIFIED(""),
  BOOL("_Bool"),
  CHAR("char"),
  INT("int"),
  FLOAT("float"),
  DOUBLE("double"),;

  private final String code;

  private CBasicType(String pCode) {
    code = pCode;
  }

  /**
   * Returns true if a type is a floating type as defined by
   * the C standard §6.2.5.
   */
  public boolean isFloatingPointType() {
    return this == FLOAT
        || this == DOUBLE;
  }

  /**
   * Returns true if a type is an integer type as defined by
   * the C standard §6.2.5.
   */
  public boolean isIntegerType() {
    return this == BOOL
        || this == CHAR
        || this == INT
        || this == UNSPECIFIED;
  }

  public String toASTString() {
    return code;
  }
}
