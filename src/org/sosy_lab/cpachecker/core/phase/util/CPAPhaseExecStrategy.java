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
package org.sosy_lab.cpachecker.core.phase.util;

import org.sosy_lab.cpachecker.core.phase.CPAPhase;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseStatus;

import java.util.List;

public interface CPAPhaseExecStrategy {

  /**
   * The main method to execute CPA phase graph
   *
   * @return the resultant status of execution
   */
  CPAPhaseStatus exec(List<CPAPhase> phase) throws Exception;

  String getName();

}
