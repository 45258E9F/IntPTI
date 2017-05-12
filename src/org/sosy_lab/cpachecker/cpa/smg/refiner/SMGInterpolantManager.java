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
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.util.refinement.InterpolantManager;

public class SMGInterpolantManager implements InterpolantManager<SMGState, SMGInterpolant> {

  private final LogManager logger;
  private final MachineModel model;
  private final SMGInterpolant initalInterpolant;

  public SMGInterpolantManager(
      MachineModel pModel,
      LogManager pLogger,
      CFA pCfa,
      int pExternalAllocationSize) {
    logger = pLogger;
    model = pModel;
    initalInterpolant = SMGInterpolant
        .createInitial(logger, model, pCfa.getMainFunction(), pExternalAllocationSize);
  }

  @Override
  public SMGInterpolant createInitialInterpolant() {
    return initalInterpolant;
  }

  @Override
  public SMGInterpolant createInterpolant(SMGState pState) {
    return pState.createInterpolant();
  }

  @Override
  public SMGInterpolant getTrueInterpolant() {
    // initial interpolant is also a true interpolant
    return initalInterpolant;
  }

  @Override
  public SMGInterpolant getFalseInterpolant() {
    return SMGInterpolant.getFalseInterpolant();
  }

}
