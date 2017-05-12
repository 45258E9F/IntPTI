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
package org.sosy_lab.cpachecker.cfa.model.java;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;

public class JMethodReturnEdge extends FunctionReturnEdge {


  public JMethodReturnEdge(
      FileLocation pFileLocation,
      FunctionExitNode pPredecessor, CFANode pSuccessor,
      JMethodSummaryEdge pSummaryEdge) {

    super(pFileLocation, pPredecessor, pSuccessor, pSummaryEdge);

  }

  @Override
  public JMethodSummaryEdge getSummaryEdge() {
    return (JMethodSummaryEdge) super.getSummaryEdge();
  }

  @Override
  public JMethodEntryNode getFunctionEntry() {
    return (JMethodEntryNode) super.getFunctionEntry();
  }
}
