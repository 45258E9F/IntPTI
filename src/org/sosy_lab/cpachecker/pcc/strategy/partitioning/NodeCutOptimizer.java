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
package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import org.sosy_lab.cpachecker.core.interfaces.pcc.FiducciaMattheysesOptimizer;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedEdge;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedNode;


public class NodeCutOptimizer implements FiducciaMattheysesOptimizer {

  @Override
  public int computeGain(
      int node, int toPartition, int[] nodeToPartition,
      WeightedGraph wGraph) {
    int gain = 0;
    gain = computeExternalDegree(node, toPartition, nodeToPartition, wGraph)
        - computeInternalDegree(node, nodeToPartition, wGraph);
    return gain;
  }

  @Override
  public int computeInternalDegree(int node, int[] nodeToPartition, WeightedGraph wGraph) {
    int internalDegree = 0;
    int partition = nodeToPartition[node];

    for (WeightedNode neighbor : wGraph.getNeighbors(node)) {
      if (nodeToPartition[neighbor.getNodeNumber()] == partition) {
        internalDegree += neighbor.getWeight();
      }
    }


    for (WeightedEdge inEdge : wGraph.getIncomingEdges(node)) {
      if (nodeToPartition[inEdge.getStartNode().getNodeNumber()] == partition) {
        internalDegree += inEdge.getWeight();
      }
    }

    return internalDegree;
  }

  @Override
  public int computeExternalDegree(
      int node, int toPartition, int[] nodeToPartition,
      WeightedGraph wGraph) {
    int externalDegree = 0;
    for (WeightedNode neighbor : wGraph.getNeighbors(node)) {
      if (nodeToPartition[neighbor.getNodeNumber()] == toPartition) {
        externalDegree += neighbor.getWeight();
      }
    }

    for (WeightedEdge inEdge : wGraph.getIncomingEdges(node)) {
      if (nodeToPartition[inEdge.getStartNode().getNodeNumber()] == toPartition) {
        externalDegree += inEdge.getWeight();
      }
    }
    return externalDegree;
  }

}
