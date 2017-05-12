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

import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * Exception thrown when a CPA cannot handle some code attached to a CFAEdge
 * because it uses features that are unsupported.
 */
public class UnsupportedCodeException extends UnrecognizedCodeException {

  private static final long serialVersionUID = -7693635256672813804L;

  public UnsupportedCodeException(String msg, CFAEdge edge, AAstNode astNode) {
    super("Unsupported feature", msg, edge, astNode);
  }

  public UnsupportedCodeException(String msg, CFAEdge cfaEdge) {
    this(msg, cfaEdge, null);
  }
}
