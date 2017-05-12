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
package org.sosy_lab.cpachecker.core.algorithm.multientry;

import org.sosy_lab.cpachecker.core.algorithm.Algorithm;

/**
 * The algorithm that supports bounded analysis. This is required in multi-entry analysis.
 */
public interface BoundedAlgorithm extends Algorithm {

  /**
   * After each bounded run, the status of algorithm should be reset.
   */
  void resetStatus();

}
