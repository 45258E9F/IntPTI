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
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.List;

/**
 * A hybrid can either perform sep-merge or join-merge. Its work mode is controlled by heuristics.
 */
public interface HybridMergeOperator extends MergeOperator {

  /**
   * Obtain the working mode of merge operator.
   * Note: since a merge operator should be reused for multiple times, its internal state should
   * be maintained carefully. The best practice is to reset the merging mode before performing a
   * new merge.
   *
   * @return merging mode
   */
  MergeMode getMergeMode();

  /**
   * A merge operator that makes use of other parallel components of abstract states.
   * This method should be implemented when merging tactics are employed. Many merging tactics
   * require location information or some other information from other components of state.
   *
   * @param state1          abstract state 1 (successor, of type T)
   * @param otherStates1    other parallel components of state 1
   * @param state2          abstract state 2 (reached state, of type T)
   * @param otherStates2    other parallel components of state 2
   * @param precision       precision of state T
   * @param otherPrecisions precisions of parallel components of states
   * @return the merging result (of type T)
   */
  AbstractState merge(
      AbstractState state1, List<AbstractState> otherStates1,
      AbstractState state2, List<AbstractState> otherStates2,
      Precision precision, List<Precision> otherPrecisions)
      throws CPAException, InterruptedException;

  /**
   * Add new tactics on current merge operator.
   * Designed for tactic sharing mechanism.
   *
   * @param tactics tactics to be shared
   */
  void addTactics(List<MergeTactic> tactics);

}
