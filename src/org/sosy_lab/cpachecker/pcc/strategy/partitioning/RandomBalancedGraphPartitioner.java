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

import org.sosy_lab.cpachecker.core.interfaces.pcc.BalancedGraphPartitioner;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;


public class RandomBalancedGraphPartitioner implements BalancedGraphPartitioner {

  @Override
  public List<Set<Integer>> computePartitioning(
      int pNumPartitions,
      PartialReachedSetDirectedGraph pGraph) {
    if (pNumPartitions <= 0 || pGraph == null) {
      throw new IllegalArgumentException(
          "Partitioniong must contain at most 1 partition. Graph may not be null.");
    }
    List<Set<Integer>> partitioning = new ArrayList<>(pNumPartitions);
    for (int i = 0; i < pNumPartitions; i++) {
      partitioning.add(new HashSet<Integer>());
    }

    Random randomGen = new Random();

    for (int i = 0; i < pGraph.getNumNodes(); i++) {
      partitioning.get(randomGen.nextInt(pNumPartitions)).add(i);
    }

    return partitioning;
  }
}
