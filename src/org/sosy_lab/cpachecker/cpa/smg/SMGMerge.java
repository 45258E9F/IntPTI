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
package org.sosy_lab.cpachecker.cpa.smg;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;


public class SMGMerge implements MergeOperator {

  private static final MergeOperator INSTANCE = new SMGMerge();

  private SMGMerge() {
    // default
  }

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {

    if (!(pState1 instanceof SMGState) || !(pState2 instanceof SMGState)) {
      return pState2;
    }

    SMGState smgState1 = (SMGState) pState1;
    SMGState smgState2 = (SMGState) pState2;

    return smgState1.joinSMG(smgState2);
  }

  public static MergeOperator getInstance() {
    return INSTANCE;
  }
}