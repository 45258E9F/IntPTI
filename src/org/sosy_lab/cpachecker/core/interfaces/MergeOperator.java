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
package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * This interface defines the merge operator used by {@link CPAAlgorithm}.
 * This operator is used to (optionally) merge newly-created abstract states
 * with existing abstract states from the reached set.
 *
 * There are several default implementations available,
 * that should be sufficient for many analyses:
 * {@link MergeSepOperator}, {@link MergeJoinOperator}.
 */
public interface MergeOperator {

  /**
   * The actual method for merging abstract states.
   * Merging abstract states is defined by weakening the state in the second parameter
   * by taking information from the state in the first parameter.
   *
   * This method may decide to not merge the states at all
   * (i.e., returning simply the state from the second input parameter),
   * or to join them by delegating to {@link AbstractDomain#join(AbstractState, AbstractState)},
   * or to somehow otherwise weaken the state from the second input parameter.
   * It may also decide to use any of these options only sometimes,
   * depending for example on the input states or the precision.
   * For trivial cases, check the default implementations of this class.
   *
   * For soundness, the resulting state needs to be as least as abstract
   * as the state in the second parameter,
   * i.e., state2 <= result <= top
   * (as defined by the {@link AbstractDomain#isLessOrEqual(AbstractState, AbstractState)} method).
   *
   * IMPORTANT NOTE:
   * Until we fix ticket #92, and the resulting state is equal to state2,
   * you need to return exactly the state2 object,
   * not another object that is only equal to it.
   *
   * @param state1    The first input state.
   * @param state2    The second input state, from which the result is produced.
   * @param precision The precision.
   * @return An abstract state between state2 and the top state.
   */
  public AbstractState merge(AbstractState state1, AbstractState state2, Precision precision)
      throws CPAException, InterruptedException;
}
