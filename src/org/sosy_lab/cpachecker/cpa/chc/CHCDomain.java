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
package org.sosy_lab.cpachecker.cpa.chc;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;


public class CHCDomain implements AbstractDomain {

  @Override
  public AbstractState join(AbstractState state1, AbstractState state2) throws CPAException {
    ((CHCState) state1).join((CHCState) state2);
    return state1;
  }

  @Override
  public boolean isLessOrEqual(AbstractState state1, AbstractState state2) throws CPAException {
    CHCState crstate1 = (CHCState) state1;
    CHCState crstate2 = (CHCState) state2;

    return ConstraintManager.subsumes(crstate1.getConstraint(), crstate2.getConstraint());
  }

}
