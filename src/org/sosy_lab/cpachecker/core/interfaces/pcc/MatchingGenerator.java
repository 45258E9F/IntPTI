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

import java.util.Map;

/**
 * Interface for computing matchings. Matchings are given implicitly by a map from node number to a
 * new node. I.e. if 2 nodes are matched, they point to the same super node.
 */
public interface MatchingGenerator {

  /**
   * Compute a matching on a weighted graph. I.e. >>each<< node has to be mapped onto another node
   * number. And a node cannot be matched twice
   *
   * @param wGraph the graph, on which a matching is computed
   * @return the computed matching
   */
  public Map<Integer, Integer> computeMatching(WeightedGraph wGraph);
}
