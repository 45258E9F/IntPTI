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
package org.sosy_lab.cpachecker.cpa.taint;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public final class TaintUtils {

  public static Set<String> getAllFunctions(CFANode mainNode) {
    Set<String> functions = new HashSet<>();

    // here the main node should be consistent with analysis entry (not necessarily be the
    // entry of main function)
    assert (mainNode instanceof FunctionEntryNode);
    Set<FunctionEntryNode> reachedFunctions = new HashSet<>();
    Stack<FunctionEntryNode> tobeProcessed = new Stack<>();
    FunctionEntryNode currentFunction;
    CFANode currentNode;
    Stack<CFANode> waitList = new Stack<>();
    HashSet<CFANode> seen = new HashSet<>();

    reachedFunctions.add((FunctionEntryNode) mainNode);
    tobeProcessed.add((FunctionEntryNode) mainNode);
    while (!tobeProcessed.isEmpty()) {
      currentFunction = tobeProcessed.pop();
      waitList.clear();
      waitList.add(currentFunction);
      seen.clear();
      seen.add(currentFunction);
      while (!waitList.isEmpty()) {
        currentNode = waitList.pop();
        for (CFAEdge outEdge : CFAUtils.leavingEdges(currentNode)) {
          if (outEdge instanceof FunctionReturnEdge) {
            continue;
          }
          if (outEdge instanceof FunctionCallEdge) {
            FunctionEntryNode newEntry = ((FunctionCallEdge) outEdge).getSuccessor();
            if (!reachedFunctions.contains(newEntry)) {
              tobeProcessed.add(newEntry);
              reachedFunctions.add(newEntry);
            }
            outEdge = ((FunctionCallEdge) outEdge).getSummaryEdge();
          }
          // if an multi-edge is encountered, nothing is to be done because there is no function
          // call or return edge
          if (!seen.contains(outEdge.getSuccessor())) {
            waitList.add(outEdge.getSuccessor());
            seen.add(outEdge.getSuccessor());
          }
        }
      }
      functions.add(currentFunction.getFunctionName());
    }
    return functions;
  }
}
