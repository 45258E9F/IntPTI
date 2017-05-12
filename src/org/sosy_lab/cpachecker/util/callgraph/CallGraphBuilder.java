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
package org.sosy_lab.cpachecker.util.callgraph;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

/**
 * @author tomgu function call graph builder We use this to build function dependencies 1. we need
 *         CFA 2. for each function call, if it does not have definition, we ignore it 3. it is
 *         singleton, you need use {@link CallGraphBuilder#getInstance()}
 */
public class CallGraphBuilder {

  private static CallGraphBuilder INSTANCE;

  private CallGraphBuilder() {
  }

  public static CallGraphBuilder getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new CallGraphBuilder();
    }
    return INSTANCE;
  }

  /**
   * Build call graph from CFA
   */
  public CallGraph buildCallGraph(CFA cfa) {
    CallGraph cg = new CallGraph(cfa);
    for (CFANode node : cfa.getAllNodes()) {
      FunctionEntryNode caller = cfa.getFunctionHead(node.getFunctionName());
      int edgeIndex = 0;
      for (edgeIndex = 0; edgeIndex < node.getNumLeavingEdges(); edgeIndex++) {
        CFAEdge edge = node.getLeavingEdge(edgeIndex);
        if (edge instanceof FunctionCallEdge) {
          FunctionCallEdge fce = (FunctionCallEdge) edge;
          cg.attach(caller, fce.getSuccessor());
        }
      }
    }
    return cg;
  }

}
