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
package org.sosy_lab.cpachecker.core.interfaces.checker;

import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import java.util.Collection;
import java.util.List;

/**
 * Many checkers support error report, and they should implement this interface.
 */
public interface CheckerWithInstantErrorReport extends GeneralChecker {

  /**
   * NOTE: in most cases only one error report is generated after one check
   *
   * @return The collection of error reports for this checker
   */
  Collection<ErrorReport> getErrorReport();

  /**
   * Checker does not accumulate error reports. Please reset after use.
   */
  void resetErrorReport();

  /**
   * Compute a collection of ARG states which are critical to current error trace.
   * For example, if a/b has DIV_BY_ZERO error, a critical state should be the nearest one that
   * makes b have value 0.
   *
   * @param path        ARG path leading to error state
   * @param nodeInError the AST node that has the error
   * @return the list of critical states, with normal order (the first node comes first)
   */
  List<ARGState> getInverseCriticalStates(ARGPath path, ErrorSpot nodeInError);

}
