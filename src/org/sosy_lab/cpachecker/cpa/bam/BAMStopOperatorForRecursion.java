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

import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.util.Collection;


/**
 * This stop-operator just forwards towards the wrapped stop-operator of the analysis.
 * Additionally, we never 'stop' at a function-call, because of the influence
 * of the predecessor of the function-call in the 'rebuild'-step.
 */
public class BAMStopOperatorForRecursion extends BAMStopOperator {

  public BAMStopOperatorForRecursion(
      StopOperator pWrappedStopOperator,
      BAMTransferRelation pTransfer) {
    super(pWrappedStopOperator, pTransfer);
  }

  @Override
  public boolean stop(
      AbstractState pState,
      Collection<AbstractState> pReached,
      Precision pPrecision)
      throws CPAException, InterruptedException {
    // we never 'stop' at a function-call, because of the influence
    // of the predecessor of the function-call in the 'rebuild'-step.
    // example that might cause problems: BallRajamani-SPIN2000-Fig1_false-unreach-call.c
    if (AbstractStates.extractLocation(pState) instanceof FunctionEntryNode) {
      return false;
    }
    return super.stop(pState, pReached, pPrecision);
  }
}
