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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;


public class WeightedGraphRandomIterator implements Iterable<WeightedNode>, Iterator<WeightedNode> {

  private final WeightedGraph wGraph;
  private final int size;
  private int current;
  private final List<Integer> permutation;

  public WeightedGraphRandomIterator(WeightedGraph wGraph) {
    super();
    this.wGraph = wGraph;
    size = wGraph.getNumNodes();
    permutation = shuffledIndices(size);
    current = 0;
  }

  @Override
  public boolean hasNext() {
    if (current < size) {
      return true;
    }
    return false;
  }

  @Nullable
  @Override
  public WeightedNode next() {
    int nodeIndex = permutation.get(current++);
    return wGraph.getNode(nodeIndex);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();

  }

  /**
   * Compute permutation of 0..n-1 to randomly iterate over an array
   *
   * @param n number of list entries
   * @return a list containing 0..n-1 in a randomized order
   */
  private List<Integer> shuffledIndices(int n) {
    List<Integer> permutation = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      permutation.add(i);
    }
    Collections.shuffle(permutation);
    return permutation;
  }

  @Override
  public Iterator<WeightedNode> iterator() {
    return new WeightedGraphRandomIterator(wGraph);
  }
}
