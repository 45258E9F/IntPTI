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
package org.sosy_lab.cpachecker.core.interfaces.conditions;

import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

/**
 * This interface marks CPAs which are able to reset a reached set to a state
 * where rerunning the CPAchecker algorithm with this CPA after adjusting the
 * CPA condition will produce the same reached set as if starting with a fresh
 * reached with only the initial state.
 */
public interface ReachedSetAdjustingCPA extends AdjustableConditionCPA {

  /**
   * Resets the given reached set to a state where rerunning the CPAchecker
   * algorithm with this CPA after adjusting the CPA condition will produce the
   * same reached set as if starting with a fresh reached set with only the
   * initial state.
   *
   * @param pReachedSet the given reached set.
   */
  void adjustReachedSet(ReachedSet pReachedSet);

}
