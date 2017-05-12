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

import java.util.Collection;

/**
 * Some checkers cannot instantly give error reports, such as dead-code analyzer.
 * Such checkers collect the global information of program, and then give error reports.
 * Thus, we should analyze its intermediate result after all analyses.
 * Errors relevant to global information have no error traces, and we will not mark the error
 * state in the ARG.
 */
public interface CheckerWithDelayedErrorReport {

  /**
   * This method captures intermediate results in checker and computes final errors.
   * Generally, this method should be invoked after the whole analysis finishes.
   *
   * @return The collection of all error reports
   */
  Collection<ErrorReport> getErrorReport();

}
