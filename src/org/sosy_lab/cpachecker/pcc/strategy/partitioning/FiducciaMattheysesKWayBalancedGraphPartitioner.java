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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PartitioningRefiner;
import org.sosy_lab.cpachecker.core.interfaces.pcc.WeightedBalancedGraphPartitioner;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.FiducciaMattheysesOptimzerFactory.OptimizationCriteria;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.GlobalGraphPartitionerHeuristicFactory.GlobalPartitioningHeuristics;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Implementation of a greedy FM/KL graph partitioning algorithm mainly based on the ideas in
 * http://glaros.dtc.umn.edu/gkhome/node/82; For framework the option of a node cut applied as well
 */
@Options(prefix = "pcc.partitioning.kwayfm")
public class FiducciaMattheysesKWayBalancedGraphPartitioner
    implements WeightedBalancedGraphPartitioner, PartitioningRefiner {

  private final LogManager logger;
  private final WeightedBalancedGraphPartitioner globalPartitioner;

  @Option(
      secure = true,
      description = "[FM-k-way] Balance criterion for pairwise optimization of partitions")
  private double balancePrecision = 1.3d;


  @Option(
      secure = true,
      description = "[FM-k-way] Partitioning method to compute initial partitioning.")
  private GlobalPartitioningHeuristics globalHeuristic =
      GlobalPartitioningHeuristics.BEST_IMPROVEMENT_FIRST;


  @Option(
      secure = true,
      description = "[FM-k-way] Local optimization criterion to be minimized druing Fiduccia/Mattheyses refinment")
  private OptimizationCriteria optimizationCriterion = OptimizationCriteria.NODECUT;


  public FiducciaMattheysesKWayBalancedGraphPartitioner(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;

    globalPartitioner = GlobalGraphPartitionerHeuristicFactory.createPartitioner(pConfig, pLogger,
        globalHeuristic);
  }

  public FiducciaMattheysesKWayBalancedGraphPartitioner(
      Configuration pConfig, LogManager pLogger,
      OptimizationCriteria criterion) throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    optimizationCriterion = criterion;
    globalPartitioner = GlobalGraphPartitionerHeuristicFactory.createPartitioner(pConfig, pLogger,
        globalHeuristic);
  }

  @Override
  public List<Set<Integer>> computePartitioning(
      int pNumPartitions,
      PartialReachedSetDirectedGraph pGraph) throws InterruptedException {
    return computePartitioning(pNumPartitions, new WeightedGraph(pGraph));

  }

  @Override
  public List<Set<Integer>> computePartitioning(int pNumPartitions, WeightedGraph wGraph)
      throws InterruptedException {
    if (pNumPartitions <= 0 || wGraph == null) {
      throw new IllegalArgumentException(
          "Partitioniong must contain at most 1 partition. Graph may not be null.");
    }
    if (pNumPartitions == 1) { //1-partitioning easy special case (Each node in the same partition)
      return wGraph.getGraphAsOnePartition();
    }
    if (pNumPartitions >= wGraph.getNumNodes()) {//Each Node has its own partition
      return wGraph.getNodesSeperatelyPartitioned(pNumPartitions);
    }

    //There is more than one partition, and at least one partition contains more than 1 node

    List<Set<Integer>> partitioning = globalPartitioner.computePartitioning(pNumPartitions, wGraph);
    refinePartitioning(partitioning, wGraph, pNumPartitions);
    removeEmptyPartitions(partitioning);
    return partitioning;
  }

  /**
   * Method to remove all empty partitions from the partitioning. Empty partitions may slow down
   * proof checking phase.
   *
   * @param partitions the partitioning to be cleaned up.
   */
  private void removeEmptyPartitions(List<Set<Integer>> partitions) {
    for (Iterator<Set<Integer>> iter = partitions.listIterator(); iter.hasNext(); ) {
      Set<Integer> partition = iter.next();
      if (partition != null && partition.isEmpty()) {
        iter.remove();
      }
    }
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitioningRefiner#refinePartitioning(java.util.List, org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph, int)
   */
  @Override
  public int refinePartitioning(
      List<Set<Integer>> partitioning,
      PartialReachedSetDirectedGraph pGraph, int numPartitions) {
    WeightedGraph wGraph = new WeightedGraph(pGraph);
    return refinePartitioning(partitioning, wGraph, numPartitions);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitioningRefiner#refinePartitioning(java.util.List, org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph, int)
   */
  @Override
  public int refinePartitioning(
      List<Set<Integer>> partitioning,
      WeightedGraph wGraph, int numPartitions) {
    int maxLoad = wGraph.computePartitionLoad(numPartitions);
    FiducciaMattheysesWeightedKWayAlgorithm fm = new FiducciaMattheysesWeightedKWayAlgorithm(
        partitioning, balancePrecision, wGraph, maxLoad, optimizationCriterion);
    int step = 1;
    int maxNumSteps = 50;
    int oldGain = 0;
    int totalGain = 0;
    int timesWithoutImprovement = 0;
    while (step <= maxNumSteps && timesWithoutImprovement < 5) {
      int newGain = fm.refinePartitioning();
      totalGain += newGain;
      if (oldGain == newGain) {
        timesWithoutImprovement++;
      }
      oldGain = newGain;
      step++;
    }
    logger.log(Level.FINE,
        String.format("[KWayFM] refinement gain %d after % d refinement steps", totalGain, step));
    return totalGain;
  }


}
