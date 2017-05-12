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
package org.sosy_lab.cpachecker.exceptions;

/**
 * Super class for all exceptions thrown by CPA operators.
 *
 * TODO This exception should probably be abstract, and specialized sub-classes
 * should be used for specific reasons.
 */
public class CPAException extends Exception {

  private static final long serialVersionUID = 6846683924964869559L;

  public CPAException(String msg) {
    super(msg);
  }

  public CPAException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
