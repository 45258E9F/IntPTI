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

import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedNode;

import java.util.Set;

/**
 * Interface providing a method to compute a nodes priority.
 * With this priority best-first-algorithm can determine  which node to be explored next.
 */
public interface BestFirstEvaluationFunction {

  /**
   * Compute priority for node on wait-list to be expanded next, depending on actual situation and
   * chosen evaluation function
   *
   * @param partition The partition predecessor was added to
   * @param priority  Priority of predecessor
   * @param node      Node which is considered
   * @param wGraph    The graph algorithm is working on
   * @return Priority to expand successor as next node
   */
  int computePriority(
      Set<Integer> partition, int priority, WeightedNode node,
      WeightedGraph wGraph);
}
