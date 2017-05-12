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

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;

/**
 * Exception thrown if a CPA cannot handle a specific CFAEdge.
 */
public class UnrecognizedCFAEdgeException extends CPATransferException {

  public UnrecognizedCFAEdgeException(CFAEdge edge) {
    super(createMessage(edge));
  }

  private static String createMessage(CFAEdge edge) {
    if (edge.getEdgeType() == CFAEdgeType.MultiEdge) {
      return "Some CPAs do not support MultiEdges. Please set the configuration option \"cfa.useMultiEdges\" to \"false\".";
    }
    return "Unknown CFA edge: " + edge.getEdgeType() + " (" + edge.getDescription() + ")";
  }

  /**
   * auto-generated UID
   */
  private static final long serialVersionUID = -5106215499745787051L;
}
