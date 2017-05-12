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
package org.sosy_lab.cpachecker.core.phase.result;

import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;


public class AlgorithmPhaseResult implements CPAPhaseResult {

  private final AlgorithmStatus status;
  private final ReachedSet reached;

  public AlgorithmPhaseResult(AlgorithmStatus pStatus, ReachedSet pReached) {
    status = pStatus;
    reached = pReached;
  }

  public AlgorithmStatus getStatus() {
    return status;
  }

  public ReachedSet getReachedSet() {
    return reached;
  }

}
