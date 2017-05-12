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
package org.sosy_lab.cpachecker.cpa.smg;

import org.sosy_lab.cpachecker.exceptions.CPATransferException;


public class SMGInconsistentException extends CPATransferException {
  private static final long serialVersionUID = -1677699207895867889L;

  public SMGInconsistentException(String msg) {
    super(msg);
  }
}