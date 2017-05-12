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
package org.sosy_lab.cpachecker.pcc.strategy.partialcertificate;


public class WeightedEdge {

  private final WeightedNode startNode;
  private final WeightedNode endNode;
  private int weight;

  public WeightedNode getStartNode() {
    return startNode;
  }

  public WeightedNode getEndNode() {
    return endNode;
  }

  public int getWeight() {
    return weight;
  }

  public void setWeight(int pWeight) {
    weight = pWeight;
  }

  public void addWeight(int pWeight) {
    weight += pWeight;
  }

  public WeightedEdge(WeightedNode pStartNode, WeightedNode pEndNode, int pWeight) {
    super();
    startNode = pStartNode;
    endNode = pEndNode;
    weight = pWeight;
  }

  /**
   * Edge represented by "start--weight-->end"
   */
  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append(startNode).append("--").append(weight).append("-->").append(endNode);
    return s.toString();
  }

}
