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
package org.sosy_lab.cpachecker.cpa.bam;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.Collection;


/**
 * This stop-operator just forwards towards the wrapped stop-operator of the analysis.
 */
public class BAMStopOperator implements StopOperator {

  private final StopOperator wrappedStop;
  private final BAMTransferRelation transfer;

  public BAMStopOperator(StopOperator pWrappedStopOperator, BAMTransferRelation pTransfer) {
    wrappedStop = pWrappedStopOperator;
    transfer = pTransfer;
  }

  @Override
  public boolean stop(
      AbstractState pState,
      Collection<AbstractState> pReached,
      Precision pPrecision)
      throws CPAException, InterruptedException {
    if (transfer.breakAnalysis) {
      return false;
    }
    return wrappedStop.stop(pState, pReached, pPrecision);
  }


}
