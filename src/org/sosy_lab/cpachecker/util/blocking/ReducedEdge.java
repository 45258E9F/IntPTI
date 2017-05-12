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
package org.sosy_lab.cpachecker.util.blocking;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

import java.util.HashSet;
import java.util.Set;

class ReducedEdge {
  private final Set<CFAEdge> summarizes = new HashSet<>();
  private final ReducedNode pointsTo;

  public void addEdge(CFAEdge pEdge) {
    this.summarizes.add(pEdge);
  }

  public void addEdge(ReducedEdge pEdge) {
    this.summarizes.addAll(pEdge.summarizes);
  }

  public ReducedEdge(ReducedNode pPointsTo) {
    this.pointsTo = pPointsTo;
  }

  public ReducedNode getPointsTo() {
    return this.pointsTo;
  }
}
