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

import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * Exception thrown when a CPA cannot handle some C code attached to a CFAEdge
 * because it uses C features that are unsupported.
 */
public class UnsupportedCCodeException extends UnrecognizedCCodeException {

  private static final long serialVersionUID = -8319167530363457020L;

  public UnsupportedCCodeException(String msg, CFAEdge edge, CAstNode astNode) {
    super("Unsupported C feature", msg, edge, astNode);
  }

  public UnsupportedCCodeException(String msg, CFAEdge cfaEdge) {
    this(msg, cfaEdge, null);
  }
}
