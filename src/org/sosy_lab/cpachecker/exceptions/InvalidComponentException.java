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

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

/**
 * Exception for cases when a class implements an interface like
 * {@link ConfigurableProgramAnalysis}, but fails to conform to some additional
 * semantic condition.
 */
public class InvalidComponentException extends CPAException {

  private static final long serialVersionUID = 3018467878727210858L;

  public InvalidComponentException(Class<?> cls, String componentType, String msg) {
    super(cls.getCanonicalName() + " is not a valid " + checkNotNull(componentType) + ": "
        + checkNotNull(msg));
  }

  public InvalidComponentException(Class<?> cpa, String componentType, Throwable cause) {
    super(cpa.getCanonicalName() + " is not a valid " + checkNotNull(componentType) + ": "
            + (cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName()),
        cause);
  }
}