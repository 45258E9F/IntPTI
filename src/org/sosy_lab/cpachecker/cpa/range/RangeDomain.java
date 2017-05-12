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
package org.sosy_lab.cpachecker.cpa.range;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public enum RangeDomain implements AbstractDomain {

  INSTANCE;

  @Override
  public AbstractState join(
      AbstractState state1, AbstractState state2) throws CPAException, InterruptedException {
    RangeState s1 = (RangeState) state1;
    RangeState s2 = (RangeState) state2;
    if (s1.equals(s2)) {
      return s2;
    }
    RangeState result = s1.join(s2);
    if (result.equals(s1)) {
      return s1;
    }
    return result;
  }

  @Override
  public boolean isLessOrEqual(
      AbstractState state1, AbstractState state2) throws CPAException, InterruptedException {
    RangeState s1 = (RangeState) state1;
    RangeState s2 = (RangeState) state2;
    return s1.isLessOrEqual(s2);
  }
}
