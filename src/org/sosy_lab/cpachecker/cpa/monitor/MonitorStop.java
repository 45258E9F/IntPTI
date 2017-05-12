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
package org.sosy_lab.cpachecker.cpa.monitor;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.Collection;
import java.util.Collections;

public class MonitorStop implements StopOperator {

  private final ConfigurableProgramAnalysis wrappedCpa;

  public MonitorStop(ConfigurableProgramAnalysis cpa) {
    this.wrappedCpa = cpa;
  }

  @Override
  public boolean stop(
      AbstractState pElement,
      Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {

    MonitorState monitorState = (MonitorState) pElement;
    if (monitorState.mustDumpAssumptionForAvoidance()) {
      return false;
    }

    AbstractState wrappedState = monitorState.getWrappedState();
    StopOperator stopOp = wrappedCpa.getStopOperator();

    for (AbstractState reachedState : pReached) {

      MonitorState monitorReachedState = (MonitorState) reachedState;
      if (monitorReachedState.mustDumpAssumptionForAvoidance()) {
        return false;
      }

      AbstractState wrappedReachedState = monitorReachedState.getWrappedState();

      if (stopOp.stop(wrappedState, Collections.singleton(wrappedReachedState), pPrecision)) {
        return true;
      }
    }

    return false;
  }
}
