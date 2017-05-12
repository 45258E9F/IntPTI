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
package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Standard merge-join operator
 */
public class MergeJoinOperator implements MergeOperator {

  private final AbstractDomain domain;

  /**
   * Creates a merge-join operator, based on the given join
   * operator
   */
  public MergeJoinOperator(AbstractDomain d) {
    this.domain = d;
  }

  @Override
  public AbstractState merge(AbstractState el1, AbstractState el2, Precision p)
      throws CPAException, InterruptedException {
    return domain.join(el1, el2);
  }

}
