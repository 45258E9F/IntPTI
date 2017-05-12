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

public class Variable implements Term {

  private String mName;

  public Variable(String pName) {
    mName = pName;
  }

  public String getName() {
    return mName;
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

    Variable lVariable = (Variable) pOther;

    return mName.equals(lVariable.mName);
  }

  @Override
  public int hashCode() {
    return mName.hashCode();
  }

  @Override
  public String toString() {
    return mName;
  }

  @Override
  public <T> T accept(TermVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

}
