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
package org.sosy_lab.cpachecker.cpa.edgeexclusion;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * This is the abstract domain of the edge exclusion CPA.
 */
enum EdgeExclusionDomain implements AbstractDomain {

  INSTANCE;

  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2) throws CPAException {
    assert pState1 == EdgeExclusionState.TOP;
    assert pState2 == EdgeExclusionState.TOP;
    return EdgeExclusionState.TOP;
  }

  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    assert pState1 == EdgeExclusionState.TOP;
    assert pState2 == EdgeExclusionState.TOP;
    return true;
  }

}
