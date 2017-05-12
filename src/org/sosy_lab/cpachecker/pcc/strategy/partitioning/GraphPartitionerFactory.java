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

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.pcc.BalancedGraphPartitioner;


public class GraphPartitionerFactory {

  private GraphPartitionerFactory() {
  }

  public static enum PartitioningHeuristics {
    RANDOM,
    DFS,
    BFS,
    OPTIMAL,
    BEST_FIRST,
    FM,
    FM_K_WAY,
    MULTILEVEL
  }

  public static BalancedGraphPartitioner createPartitioner(
      final LogManager pLogger,
      final PartitioningHeuristics pHeuristic,
      final ShutdownNotifier pShutdownNotifier,
      final Configuration pConfig) throws InvalidConfigurationException {
    switch (pHeuristic) {
      case DFS:
        return new ExplorationOrderBalancedGraphPartitioner(true, pShutdownNotifier);
      case BFS:
        return new ExplorationOrderBalancedGraphPartitioner(false, pShutdownNotifier);
      case OPTIMAL:
        return new ExponentialOptimalBalancedGraphPartitioner(pShutdownNotifier);
      case BEST_FIRST:
        return new BestFirstWeightedBalancedGraphPartitioner(pConfig, pLogger);
      case FM:
        return new FiducciaMattheysesBalancedGraphPartitioner(pConfig, pLogger, pShutdownNotifier);
      case FM_K_WAY:
        return new FiducciaMattheysesKWayBalancedGraphPartitioner(pConfig, pLogger);
      case MULTILEVEL:
        return new MultilevelBalancedGraphPartitioner(pConfig, pLogger);
      default: // RANDOM
        return new RandomBalancedGraphPartitioner();
    }
  }

}
