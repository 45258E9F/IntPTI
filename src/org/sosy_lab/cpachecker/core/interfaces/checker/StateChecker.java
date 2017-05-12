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
package org.sosy_lab.cpachecker.core.interfaces.checker;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;

/**
 * Check if current operation has bug given the initial state
 * NOTE: some checkers do not require visiting expression.
 */
public interface StateChecker<T extends AbstractState> extends GeneralChecker {

  /**
   * The core method that checks (and refines) current state
   *
   * @param postState       the state after strengthen
   * @param postOtherStates all state components after strengthen
   * @param cfaEdge         the CFA edge
   * @param newPostStates   the collection of resultant states
   */
  void checkAndRefine(
      T postState,
      Collection<AbstractState> postOtherStates,
      CFAEdge cfaEdge,
      Collection<T> newPostStates)
      throws CPATransferException;

}
