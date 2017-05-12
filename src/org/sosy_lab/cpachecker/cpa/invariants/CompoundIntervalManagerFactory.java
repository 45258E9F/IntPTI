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
package org.sosy_lab.cpachecker.cpa.invariants;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;


public interface CompoundIntervalManagerFactory {

  CompoundIntervalManager createCompoundIntervalManager(MachineModel pMachineModel, Type pType);

  CompoundIntervalManager createCompoundIntervalManager(BitVectorInfo pBitVectorInfo);

}
