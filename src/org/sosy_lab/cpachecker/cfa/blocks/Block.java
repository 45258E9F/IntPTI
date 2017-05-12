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
package org.sosy_lab.cpachecker.cfa.blocks;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cfa.model.CFANode;

import java.util.Collections;
import java.util.Set;

/**
 * Represents a block as described in the BAM paper.
 */
public class Block {

  private final ImmutableSet<ReferencedVariable> referencedVariables;
  private final ImmutableSet<CFANode> callNodes;
  private final ImmutableSet<CFANode> returnNodes;
  private final ImmutableSet<CFANode> nodes;

  public Block(
      Set<ReferencedVariable> pReferencedVariables,
      Set<CFANode> pCallNodes, Set<CFANode> pReturnNodes, Set<CFANode> allNodes) {

    referencedVariables = ImmutableSet.copyOf(pReferencedVariables);
    callNodes = ImmutableSet.copyOf(pCallNodes);
    returnNodes = ImmutableSet.copyOf(pReturnNodes);
    nodes = ImmutableSet.copyOf(allNodes);
  }

  public Set<CFANode> getCallNodes() {
    return Collections.unmodifiableSet(callNodes);
  }

  public CFANode getCallNode() {
    assert callNodes.size() == 1;
    return callNodes.iterator().next();
  }

  public Set<ReferencedVariable> getReferencedVariables() {
    return referencedVariables;
  }

  public Set<CFANode> getNodes() {
    return nodes;
  }

  public boolean isReturnNode(CFANode pNode) {
    return returnNodes.contains(pNode);
  }

  public Set<CFANode> getReturnNodes() {
    return returnNodes;
  }

  public boolean isCallNode(CFANode pNode) {
    return callNodes.contains(pNode);
  }

  @Override
  public String toString() {
    return "Block " +
        "(CallNodes: " + callNodes + ") " +
        "(Nodes: " + (nodes.size() < 10 ? nodes : "[#=" + nodes.size() + "]") + ") " +
        "(ReturnNodes: " + returnNodes + ")";
  }
}
