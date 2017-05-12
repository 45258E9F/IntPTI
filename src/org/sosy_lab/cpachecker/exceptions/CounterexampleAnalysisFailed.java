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
package org.sosy_lab.cpachecker.exceptions;

/**
 * Exception for cases when a counterexample could not be analyzed for its
 * feasibility.
 */
public class CounterexampleAnalysisFailed extends CPAException {

  private static final long serialVersionUID = 1739510661568141393L;

  public CounterexampleAnalysisFailed(String msg) {
    super("Counterexample could not be analyzed: " + msg);
  }

  public CounterexampleAnalysisFailed(String msg, Throwable cause) {
    super("Counterexample could not be analyzed: " + msg, cause);
  }
}
