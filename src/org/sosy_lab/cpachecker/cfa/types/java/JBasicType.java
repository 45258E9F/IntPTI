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
package org.sosy_lab.cpachecker.cfa.types.java;

/**
 * A basic Java type. This includes mostly primitive types, but also types like <code>null</code>
 * or even a special unspecified type.
 */
public enum JBasicType {


  UNSPECIFIED(""),
  NULL("null"),
  VOID("void"),
  BYTE("byte"),
  SHORT("short"),
  BOOLEAN("boolean"),
  CHAR("char"),
  INT("int"),
  LONG("long"),
  FLOAT("float"),
  DOUBLE("double"),;

  private final String code;

  private JBasicType(String pCode) {
    code = pCode;
  }

  public boolean isFloatingPointType() {
    return this == FLOAT
        || this == DOUBLE;
  }

  public boolean isIntegerType() {
    return this == BYTE
        || this == CHAR
        || this == SHORT
        || this == INT
        || this == LONG;
  }

  /**
   * Returns an unambiguous String representation of this type.
   * from all other <code>JBasicType</code> enum constants.
   *
   * @return a unique String representation of this type
   */
  public String toASTString() {
    return code;
  }

}
