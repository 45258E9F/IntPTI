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
package org.sosy_lab.cpachecker.util.ci;

import org.sosy_lab.cpachecker.exceptions.CPAException;


public class AppliedCustomInstructionParsingFailedException extends CPAException {

  private static final long serialVersionUID = -1974068616247550237L;

  public AppliedCustomInstructionParsingFailedException(final String pMsg) {
    super(pMsg);
  }

  public AppliedCustomInstructionParsingFailedException(final String msg, final Throwable cause) {
    super(msg, cause);
  }

}