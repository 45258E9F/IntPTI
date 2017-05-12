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
package org.sosy_lab.cpachecker.core.interfaces.conditions;


/**
 * This interface marks CPAs which implement "conditions" as presented in the
 * technical report MIP-1107 ("Conditional Model Checking") by Beyer et. al.
 *
 * In addition to the paper, CPAs which implement this interface are conditions
 * which are "adjustable", enabling an iterative analysis with a different
 * (higher) threshold in each iteration. The algorithm implementing this is
 * {@link org.sosy_lab.cpachecker.core.algorithm.RestartWithConditionsAlgorithm}.
 */
public interface AdjustableConditionCPA {

  /**
   * Select the next higher threshold for the condition(s).
   *
   * @return false if precision could not be adjusted, for example because a user-specified upper
   * hard limit was reached, and analysis should terminate; true otherwise
   */
  boolean adjustPrecision();

}
