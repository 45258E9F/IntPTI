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
package org.sosy_lab.cpachecker.util.access;


public class FieldAccessSegment implements PathSegment {
  private final String field;

  public FieldAccessSegment(String pField) {
    super();
    field = pField;
  }

  @Override
  public String getName() {
    return field;
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    }
    if (that == null || !getClass().equals(that.getClass())) {
      return false;
    }
    FieldAccessSegment other = (FieldAccessSegment) that;
    if (!field.equals(other.field)) {
      return false;
    }
    return true;
  }

}
