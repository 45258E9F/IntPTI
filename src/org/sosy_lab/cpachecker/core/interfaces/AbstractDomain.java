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
package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public interface AbstractDomain {

  /**
   * Returns the smallest state of the lattice that is greater than both
   * states (the join).
   *
   * This is an optional method. If a domain is expected to be used only with
   * merge-sep, it does not have to provide an implementation of this method.
   * This method should then throw an {@link UnsupportedOperationException}.
   *
   * If {@link MergeJoinOperator} is used,
   * please also follow the note in the documentation of
   * {@link MergeOperator#merge(AbstractState, AbstractState, Precision)}.
   *
   * @param state1 an abstract state
   * @param state2 an abstract state
   * @return the join of state1 and state2
   * @throws CPAException                  If any error occurred.
   * @throws UnsupportedOperationException If this domain does not provide a join method.
   * @throws InterruptedException          If the operation could not complete due to a shutdown
   *                                       request.
   */
  public AbstractState join(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException;

  /**
   * Returns true if state1 is less or equal than state2 with respect to
   * the lattice.
   *
   * @param state1 an abstract state
   * @param state2 an abstract state
   * @return (state1 <= state2)
   * @throws CPAException         If any error occurred.
   * @throws InterruptedException If the operation could not complete due to a shutdown request.
   */
  public boolean isLessOrEqual(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException;

}
