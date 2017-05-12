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
package org.sosy_lab.cpachecker.cpa.statistics;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * The Domain of the StatisticsCPA; delegates its work to the states, which in turn delegate to
 * StatisticsStateFactory.
 */
public class StatisticsCPADomain implements AbstractDomain {

  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2) throws CPAException {
    StatisticsState state1 = (StatisticsState) pState1;
    StatisticsState state2 = (StatisticsState) pState2;
    assert (state1.getLocationNode().equals(state2.getLocationNode()))
        : "can only merge on the same location";
    return state1.mergeState(state2);
  }

  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    StatisticsState state1 = (StatisticsState) pState1;
    StatisticsState state2 = (StatisticsState) pState2;

    return state2.containsPrevious(state1);
  }

}
