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
package org.sosy_lab.cpachecker.util.predicates.ldd;

import edu.cmu.sei.rtss.jldd.swig.JLDD;


public class LDD {

  private final int lddPtr;

  private final LDDFactory factory;

  LDD(LDDFactory factory, int lddPtr) {
    this.lddPtr = lddPtr;
    this.factory = factory;
  }

  int getLddPtr() {
    return this.lddPtr;
  }

  @Override
  public boolean equals(Object ldd) {
    if (this == ldd) {
      return true;
    }
    if (ldd instanceof LDD) {
      return this.lddPtr == ((LDD) ldd).lddPtr;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.lddPtr;
  }

  public boolean isOne() {
    return equals(this.factory.one());
  }

  public boolean isZero() {
    return equals(this.factory.zero());
  }

  public LDD imp(LDD pLdd) {
    return negate().or(pLdd);
  }

  public LDD negate() {
    return new LDD(this.factory, JLDD.jldd_not(this.lddPtr));
  }

  public LDD or(LDD pLdd) {
    return this.factory.createOr(this, pLdd);
  }

  public LDD and(LDD pLdd) {
    return this.factory.createAnd(this, pLdd);
  }

  public LDD xor(LDD pLdd) {
    return this.factory.createXor(this, pLdd);
  }

  public LDD exists(LDD pLdd) {
    return this.factory.createExists(this, pLdd);
  }

  public LDD pointerComplement() {
    int complementPtr = this.lddPtr | 01;
    return new LDD(this.factory, complementPtr);
  }

  public boolean isPointerComplement() {
    return (this.lddPtr & 01) != 0;
  }

  public LDD pointerNot() {
    int complementPtr = this.lddPtr ^ 01;
    return new LDD(this.factory, complementPtr);
  }

  public LDD conditionalPointerNot(boolean condition) {
    if (condition) {
      return pointerNot();
    }
    return this;
  }

  public LDD getE() {
    return new LDD(this.factory, JLDD.jldd_E(this.lddPtr));
  }

  public LDD getT() {
    return new LDD(this.factory, JLDD.jldd_T(this.lddPtr));
  }

  public LDD high() {
    LDD t = getT();
    return t.conditionalPointerNot(t.isPointerComplement());
  }

  public LDD regular() {
    return new LDD(this.factory, JLDD.jldd_regular(lddPtr));
  }

  public int getVar() {
    return JLDD.jldd_read_node_index(JLDD.jldd_regular(lddPtr));
  }

  public LDD low() {
    LDD e = getE();
    return e.conditionalPointerNot(e.isPointerComplement());
  }

  public LDD makeIfThenElse(LDD positive, LDD negative) {
    return this.factory.createIfThenElse(this, positive, negative);
  }

}
