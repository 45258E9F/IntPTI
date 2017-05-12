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

/**
 * Interface for classes which are used to compute the optimization criterion.
 */
public interface FiducciaMattheysesOptimizer {

  /**
   * Compute the gain of a node move, i.e usually external-internal degree
   *
   * @param node        Node which might be moved to another partition
   * @param toPartition Partition where the node probably is moved
   * @param wGraph      the graph node belongs to
   * @return its gain with respect to the chosen move
   */
  public int computeGain(int node, int toPartition, int[] nodeToPartition, WeightedGraph wGraph);

  /**
   * Compute a node's internal degree, i.e. an integer describing its connectivity into its own
   * partition
   *
   * @param node            node whose degree is computed
   * @param nodeToPartition a map from a nodes onto their belonging partition
   * @param wGraph          the graph on which this stuff is computed
   * @return the node's internal degree
   */
  public int computeInternalDegree(int node, int[] nodeToPartition, WeightedGraph wGraph);

  /**
   * Compute a nodes extends degree, i.e the integer which describes the connectivity into the
   * external partition.
   *
   * @param node            node whose degree is computed
   * @param toPartition     partition where node might be moved
   * @param nodeToPartition a map from a nodes onto their belonging partition
   * @param wGraph          the graph on which this stuff is computed
   * @return the node's external degree
   */
  public int computeExternalDegree(
      int node, int toPartition, int[] nodeToPartition,
      WeightedGraph wGraph);
}
