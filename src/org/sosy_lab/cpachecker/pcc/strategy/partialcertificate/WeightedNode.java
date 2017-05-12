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
package org.sosy_lab.cpachecker.pcc.strategy.partialcertificate;

public class WeightedNode {

  private final int nodeNumber;
  private final int weight;

  public int getNodeNumber() {
    return nodeNumber;
  }

  public int getWeight() {
    return weight;
  }

  public WeightedNode(int pNode, int pWeight) {
    super();
    nodeNumber = pNode;
    weight = pWeight;
  }

  /**
   * Node represented by [node(W: weight)]
   */
  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append(nodeNumber).append("(W:").append(weight).append(")");
    return s.toString();
  }
}
