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
package org.sosy_lab.cpachecker.cfa.model;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;


public class FunctionReturnEdge extends AbstractCFAEdge {

  private final FunctionSummaryEdge summaryEdge;

  protected FunctionReturnEdge(
      FileLocation pFileLocation,
      FunctionExitNode pPredecessor, CFANode pSuccessor,
      FunctionSummaryEdge pSummaryEdge) {

    super("", pFileLocation, pPredecessor, pSuccessor);
    summaryEdge = pSummaryEdge;
  }

  public FunctionSummaryEdge getSummaryEdge() {
    return summaryEdge;
  }

  @Override
  public String getCode() {
    return "";
  }

  @Override
  public String getDescription() {
    return "Return edge from " + getPredecessor().getFunctionName() + " to " + getSuccessor()
        .getFunctionName();
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.FunctionReturnEdge;
  }

  @Override
  public FunctionExitNode getPredecessor() {
    // the constructor enforces that the predecessor is always a FunctionExitNode
    return (FunctionExitNode) super.getPredecessor();
  }

  public FunctionEntryNode getFunctionEntry() {
    return summaryEdge.getFunctionEntry();
  }
}
