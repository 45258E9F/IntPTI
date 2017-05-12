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
package org.sosy_lab.cpachecker.cpa.bam;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class BAMMergeOperator implements MergeOperator {

  private MergeOperator wrappedMergeOp;
  private BAMTransferRelation trans;

  public BAMMergeOperator(MergeOperator pWrappedMerge, BAMTransferRelation pTrans) {
    wrappedMergeOp = pWrappedMerge;
    trans = pTrans;
  }

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {
    return trans.attachAdditionalInfoToCallNode(wrappedMergeOp.merge(pState1, pState2, pPrecision));
  }

}
