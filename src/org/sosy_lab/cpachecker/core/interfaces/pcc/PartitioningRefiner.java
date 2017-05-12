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
package org.sosy_lab.cpachecker.core.interfaces.pcc;

import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;

import java.util.List;
import java.util.Set;

/**
 * Interface for graph partitioning algorithms, which are able to refine a partitioning. Refining a
 * partitioning means, that the partitioning is improved, with respect to some chosen optimiization
 * criterium. Usually this is done by some local criteria.
 */
public interface PartitioningRefiner {

  /**
   * Refine an initially given partitioning on a given graph. Usually improving a chosen local
   * criterion.
   *
   * Operations are directly applied on the partitioning, i.e. it is changed.
   *
   * @param partitioning  initial partitioning to be improved.
   * @param pGraph        given graph
   * @param numPartitions number of partitions to be created
   * @return the total gain according to the improvements
   */
  int refinePartitioning(
      List<Set<Integer>> partitioning,
      PartialReachedSetDirectedGraph pGraph, int numPartitions);

  /**
   * Refine an initially given partitioning on a given weighted graph. Usually improving a chosen
   * local criterion.
   *
   * Operations are directly applied on the partitioning, i.e. it is changed.
   *
   * @param partitioning  initial partitioning to be improved.
   * @param wGraph        a given weighted graph
   * @param numPartitions number of partitions to be created
   * @return the total gain according to the improvements
   */
  int refinePartitioning(
      List<Set<Integer>> partitioning,
      WeightedGraph wGraph, int numPartitions);

}