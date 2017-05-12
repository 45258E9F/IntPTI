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
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PartitioningRefiner;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.FiducciaMattheysesOptimzerFactory.OptimizationCriteria;

public class PartitioningRefinerFactory {

  private PartitioningRefinerFactory() {
  }

  public static enum RefinementHeuristics {
    FM_NODECUT,
    FM_EDGECUT
  }

  public static PartitioningRefiner createRefiner(
      final Configuration pConfig,
      final LogManager pLogger,
      final RefinementHeuristics pHeuristic)
      throws InvalidConfigurationException {
    switch (pHeuristic) {
      case FM_EDGECUT:
        return new FiducciaMattheysesKWayBalancedGraphPartitioner(pConfig, pLogger,
            OptimizationCriteria.EDGECUT);
      default: //FM_K_WAY (NODE_CUT)
        return new FiducciaMattheysesKWayBalancedGraphPartitioner(pConfig, pLogger,
            OptimizationCriteria.NODECUT);
    }
  }
}
