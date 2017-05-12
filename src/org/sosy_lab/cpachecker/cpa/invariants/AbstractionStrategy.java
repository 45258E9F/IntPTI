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
package org.sosy_lab.cpachecker.cpa.invariants;

interface AbstractionStrategy {

  /**
   * Gets an abstraction state with no specific data.
   *
   * @return an abstraction state with no specific data.
   */
  public AbstractionState getAbstractionState();

  /**
   * Gets an abstraction state that represents the successor of the given
   * abstraction state.
   *
   * @param pPrevious the preceding state.
   * @return an abstraction state that represents the successor of the given abstraction state.
   */
  public AbstractionState getSuccessorState(AbstractionState pPrevious);

  /**
   * Gets an abstraction state that resembles the given abstraction state as
   * close as this factory allows.
   *
   * @param pOther the state to be represented.
   * @return an abstraction state that resembles the given abstraction state as close as this
   * factory allows.
   */
  public AbstractionState from(AbstractionState pOther);

}