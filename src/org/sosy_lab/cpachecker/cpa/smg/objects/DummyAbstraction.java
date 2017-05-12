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
package org.sosy_lab.cpachecker.cpa.smg.objects;


public class DummyAbstraction extends SMGObject implements SMGAbstractObject {

  public DummyAbstraction(SMGObject pPrototype) {
    super(pPrototype);
  }

  @Override
  public boolean matchGenericShape(SMGAbstractObject pOther) {
    return pOther instanceof DummyAbstraction;
  }

  @Override
  public boolean matchSpecificShape(SMGAbstractObject pOther) {
    return true;
  }

  @Override
  public boolean isAbstract() {
    return true;
  }

  @Override
  public SMGObject copy() {
    return new DummyAbstraction(this);
  }
}
