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
package org.sosy_lab.cpachecker.cpa.invariants;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;


public enum CompoundMathematicalIntervalManagerFactory implements CompoundIntervalManagerFactory {

  INSTANCE;

  @Override
  public CompoundIntervalManager createCompoundIntervalManager(
      MachineModel pMachineModel,
      Type pType) {
    return CompoundMathematicalIntervalManager.INSTANCE;
  }

  @Override
  public CompoundIntervalManager createCompoundIntervalManager(BitVectorInfo pBitVectorInfo) {
    return CompoundMathematicalIntervalManager.INSTANCE;
  }

}
