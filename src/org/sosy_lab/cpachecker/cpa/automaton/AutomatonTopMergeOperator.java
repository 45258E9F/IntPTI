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
package org.sosy_lab.cpachecker.cpa.automaton;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class AutomatonTopMergeOperator implements MergeOperator {

  private final AbstractDomain domain;
  private final AbstractState topState;

  public AutomatonTopMergeOperator(AbstractDomain pDomain, AbstractState pTopState) {
    this.domain = pDomain;
    this.topState = pTopState;
  }

  @Override
  public AbstractState merge(AbstractState el1, AbstractState el2, Precision p)
      throws CPAException, InterruptedException {

    boolean anyAutomatonTop =
        domain.isLessOrEqual(topState, el1)
            || domain.isLessOrEqual(topState, el2);

    if (anyAutomatonTop) {
      return topState;
    } else {
      return el2;
    }
  }

}
