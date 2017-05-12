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
package org.sosy_lab.cpachecker.core.summary.instance.arith.ast;


import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.apron.ApronState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class LinearVariable {

  private MemoryLocation memoryLocation;
  private CType type;

  public LinearVariable(MemoryLocation memoryLocation, CType type) {
    this.setMemoryLocation(memoryLocation);
    this.setType(type);
  }

  public static LinearVariable of(MemoryLocation memoryLocation, ApronState state) {
    CType type = state.getVariableToCTypeMap().get(memoryLocation);
    return new LinearVariable(memoryLocation, type);
  }

  public MemoryLocation getMemoryLocation() {
    return memoryLocation;
  }

  public void setMemoryLocation(MemoryLocation pMemoryLocation) {
    memoryLocation = pMemoryLocation;
  }

  public CType getType() {
    return type;
  }

  public void setType(CType pType) {
    type = pType;
  }

  @Override
  public String toString() {
    String result = memoryLocation.toString() + "(" + type.toString() + ")";
    return result;
  }

}
