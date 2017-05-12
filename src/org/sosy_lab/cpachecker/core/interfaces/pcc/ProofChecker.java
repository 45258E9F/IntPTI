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
package org.sosy_lab.cpachecker.core.interfaces.pcc;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;
import java.util.List;

/**
 * Interface for classes representing an analysis that can be proof checked.
 */
public interface ProofChecker {
  /**
   * Checks whether the given set of abstract successors correctly over-approximates the set of
   * concrete successors the concretisations of the given abstract state has with respect to the
   * given CFA edge. If the given edge is <code>null</code> all CFA edges have to be considered.
   *
   * @param state       abstract state with current state
   * @param otherStates complete components of current abstract state
   * @param cfaEdge     null or an edge of the CFA
   * @param successors  list of all successors of the current state (may be empty)   @return
   *                    <code>true</code> if successors are valid over-approximation;
   *                    <code>false</code>, otherwise.
   */
  public boolean areAbstractSuccessors(
      AbstractState state,
      List<AbstractState> otherStates,
      CFAEdge cfaEdge,
      Collection<? extends AbstractState> successors)
      throws CPATransferException, InterruptedException;

  /**
   * Checks whether the given state is covered by an other state. That is, the set of
   * concretisations of the state has to be a subset of the set of concretisations of the other
   * state.
   */
  public boolean isCoveredBy(AbstractState state, AbstractState otherState)
      throws CPAException, InterruptedException;
}
