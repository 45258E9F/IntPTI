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
package org.sosy_lab.cpachecker.core.interfaces.merge;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.List;

/**
 * Interface for merge tactic.
 */
public interface MergeTactic {

  /**
   * Merge two abstract states.
   * Note: the merged state should be the new state in place of the reached state.
   *
   * @return merged abstract state, should have the same type as successor and reached states
   */
  AbstractState merge(
      AbstractState successor, List<AbstractState> successorOthers,
      AbstractState reached, List<AbstractState> reachedOthers,
      Precision precision, List<Precision> otherPrecisions)
      throws CPAException, InterruptedException;

}
