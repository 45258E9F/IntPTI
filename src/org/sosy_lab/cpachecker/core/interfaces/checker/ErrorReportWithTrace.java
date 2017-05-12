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

import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import java.util.List;

/**
 * Error report with detailed execution path
 */
public interface ErrorReportWithTrace extends ErrorReport {

  /**
   * Get the critical states on the error trace.
   * NOTE: by default, this method returns a single error state.
   *
   * @return a list of ARG states in error trace.
   */
  List<ARGState> getCriticalErrorStates();

  /**
   * Get the error trace on ARG level.
   * NOTE: by default, this method returns a singleton list with only one error state.
   *
   * @return an ARG path that leads to the error state
   */
  ARGPath getErrorTrace();

  /** the following three methods can have default implementations **/

  /**
   * Update the error trace given the critical states
   * This method is for convenience of universal manipulation of traced errors
   *
   * @param criticalStates a list of all critical ARG states
   */
  void updateCriticalStates(List<ARGState> criticalStates);

  /**
   * Update the error path
   * The updated path can be errorTrace, or the truncation of errorTrace. However, the resultant
   * path should contains all critical states and error state.
   *
   * @param errorTrace the error trace in ARG path form
   */
  void updateErrorTrace(ARGPath errorTrace);

  /**
   * Get the checker that yields this error report.
   * We need this checker to generate a trace on ARG.
   *
   * @return a checker
   */
  CheckerWithInstantErrorReport getChecker();

}
