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
package org.sosy_lab.cpachecker.cfa.types.java;

/**
 * Special type for the Java value <code>null</code>.
 */
public class JNullType implements JReferenceType {

  private static final long serialVersionUID = 3755021493601316268L;

  @Override
  public String toASTString(String pDeclarator) {
    return "null";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    return prime * result;
  }

  /**
   * Returns whether a given object equals this object.
   * All <code>JNullType</code> objects equal each other.
   *
   * @param obj the object to compare with this object
   * @return <code>true</code> if the given object equals this object, <code>false</code> otherwise
   */
  @Override
  public boolean equals(Object obj) {
    return obj instanceof JNullType;
  }

  @Override
  public String toString() {
    return toASTString("");
  }
}
