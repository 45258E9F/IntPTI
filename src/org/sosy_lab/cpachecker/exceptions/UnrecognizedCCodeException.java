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

import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

import javax.annotation.Nullable;

/**
 * Exception thrown when a CPA cannot handle some C code attached to a CFAEdge.
 */
public class UnrecognizedCCodeException extends UnrecognizedCodeException {

  private static final long serialVersionUID = -8319167530363457020L;

  protected UnrecognizedCCodeException(
      String msg1, @Nullable String msg2,
      @Nullable CFAEdge edge, @Nullable AAstNode astNode) {
    super(msg1, msg2, edge, astNode);
  }

  public UnrecognizedCCodeException(String msg2, CFAEdge edge, CAstNode astNode) {
    super(msg2, edge, astNode);
  }

  public UnrecognizedCCodeException(String msg2, CFAEdge edge) {
    super(msg2, edge);
  }

  public UnrecognizedCCodeException(String msg2, CAstNode astNode) {
    super(msg2, astNode);
  }
}
