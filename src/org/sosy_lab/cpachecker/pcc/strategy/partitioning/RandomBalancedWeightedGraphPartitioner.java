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
package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import org.sosy_lab.cpachecker.core.interfaces.pcc.WeightedBalancedGraphPartitioner;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;


public class RandomBalancedWeightedGraphPartitioner extends RandomBalancedGraphPartitioner
    implements WeightedBalancedGraphPartitioner {

  @Override
  public List<Set<Integer>> computePartitioning(int pNumPartitions, WeightedGraph wGraph)
      throws InterruptedException {
    if (pNumPartitions <= 0 || wGraph == null) {
      throw new IllegalArgumentException(
          "Partitioniong must contain at most 1 partition. Graph may not be null.");
    }
    if (pNumPartitions == 1) { //1-partitioning easy special case (all nodes in one partition)
      //No expensive computations to do
      return wGraph.getGraphAsOnePartition();
    }
    if (pNumPartitions >= wGraph.getNumNodes()) {//Each Node has its own partition
      return wGraph.getNodesSeperatelyPartitioned(pNumPartitions);
    }

    //There is more than one partition, and at least one partition contains more than 1 node

    List<Set<Integer>> partitioning = new ArrayList<>(pNumPartitions);
    for (int i = 0; i < pNumPartitions; i++) {
      partitioning.add(new HashSet<Integer>());
    }

    Random randomGen = new Random();

    for (int i = 0; i < wGraph.getNumNodes(); i++) {
      partitioning.get(randomGen.nextInt(pNumPartitions)).add(i);
    }

    return partitioning;
  }

}
