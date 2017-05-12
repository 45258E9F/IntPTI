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
package org.sosy_lab.cpachecker.util.predicates.simpleformulas;

public class Constant implements Term {

  private int mValue;

  public Constant(int pValue) {
    mValue = pValue;
  }

  public int getValue() {
    return mValue;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    if (pOther == null) {
      return false;
    }

    if (!(pOther.getClass().equals(getClass()))) {
      return false;
    }

    Constant lConstant = (Constant) pOther;

    return (mValue == lConstant.mValue);
  }

  @Override
  public int hashCode() {
    return mValue;
  }

  @Override
  public String toString() {
    return "" + mValue;
  }

  @Override
  public <T> T accept(TermVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

}
