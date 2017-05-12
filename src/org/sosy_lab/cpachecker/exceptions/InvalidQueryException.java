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
 * Signals that the check method of a AbstractState has
 * been passed an Argument that could not be evaluated.
 */
public class InvalidQueryException extends CPATransferException {
  private static final long serialVersionUID = 3410773868391514648L;

  /**
   * Constructs an {@code InvalidQueryException} with the specified detail message.
   *
   * @param message The detail message (which is saved for later retrieval by the {@link
   *                #getMessage()} method)
   */
  public InvalidQueryException(String message) {
    super(message);
  }

  public InvalidQueryException(String message, Throwable cause) {
    super(message, cause);
  }
}
