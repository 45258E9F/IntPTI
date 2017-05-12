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

import com.google.common.collect.Multimap;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.util.Collection;
import java.util.HashSet;


public final class PartitioningUtils {

  private PartitioningUtils() {
  }

  public static boolean areElementsCoveredByPartitionElement(
      final Collection<AbstractState> pInOtherPartitions,
      Multimap<CFANode, AbstractState> pInPartition,
      final StopOperator pStop,
      final Precision pPrec)
      throws CPAException, InterruptedException {
    HashSet<AbstractState> partitionNodes = new HashSet<>(pInPartition.values());

    for (AbstractState outState : pInOtherPartitions) {
      if (!partitionNodes.contains(outState)
          && !pStop
          .stop(outState, pInPartition.get(AbstractStates.extractLocation(outState)), pPrec)) {
        return false;
      }
    }

    return true;
  }

}
