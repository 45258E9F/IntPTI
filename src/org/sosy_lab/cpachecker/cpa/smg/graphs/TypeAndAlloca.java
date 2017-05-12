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
package org.sosy_lab.cpachecker.cpa.smg.graphs;

import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * A wrapper class contains C type and allocation status of one memory location
 */
public class TypeAndAlloca {

  private CType type;
  private final boolean isDynamic;

  public static final boolean DYNAMIC = true;
  public static final boolean STATIC = false;

  public TypeAndAlloca(CType pType, boolean pIsDynamic) {
    type = pType;
    isDynamic = pIsDynamic;
  }

  public CType getType() {
    return type;
  }

  public boolean getAlloca() {
    return isDynamic;
  }

  public void updateType(CType newType) {
    type = newType;
  }

  @Override
  public String toString() {
    return type.toString() + ", " + (isDynamic ? "Dynamic" : "Static");
  }
}
