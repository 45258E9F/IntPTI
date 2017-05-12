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
 * Super class for all exceptions thrown from transfer relation.
 */
public class CPATransferException extends CPAException {

  private static final long serialVersionUID = -7851950254941139295L;

  public CPATransferException(String msg) {
    super(msg);
  }

  public CPATransferException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
