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
package org.sosy_lab.cpachecker.cpa.boundary.info;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

import java.util.Collection;
import java.util.Collections;

public class FunctionBoundedInfo implements BoundedInfo<String> {

  private String function;
  private CFunctionSummaryEdge summaryEdge;

  private FunctionBoundedInfo(String pFunction, CFunctionSummaryEdge pEdge) {
    function = pFunction;
    summaryEdge = pEdge;
  }

  public static BoundedInfo<?> of(String pFunction, CFANode pCallerNode) {
    CFunctionSummaryEdge summaryEdge = (CFunctionSummaryEdge) pCallerNode.getLeavingSummaryEdge();
    if (summaryEdge == null) {
      return EmptyBoundedInfo.EMPTY;
    }
    assert pFunction.equals(summaryEdge.getFunctionEntry().getFunctionName()) :
        "Inconsistent function declaration on summary edge " + summaryEdge;
    return new FunctionBoundedInfo(pFunction, summaryEdge);
  }

  @Override
  public String getBoundedObject() {
    return function;
  }

  @Override
  public CFAEdge getEntry() {
    CFAEdge edge = CFAUtils.getConnectingEdge(summaryEdge.getPredecessor(), summaryEdge
        .getFunctionEntry(), FunctionCallEdge.class);
    if (edge == null) {
      throw new IllegalArgumentException("There should be a function call edge between the "
          + "predecessor of summary edge and the function entry node");
    }
    return edge;
  }

  @Override
  public Collection<CFAEdge> getExit() {
    CFANode successor = summaryEdge.getSuccessor();
    CFANode exitNode = summaryEdge.getFunctionEntry().getExitNode();
    CFAEdge edge = CFAUtils.getConnectingEdge(exitNode, successor, FunctionReturnEdge.class);
    if (edge == null) {
      throw new IllegalArgumentException("There should be a function return edge between the "
          + "function exit node and the successor of the summary edge");
    }
    return Collections.singleton(edge);
  }
}
