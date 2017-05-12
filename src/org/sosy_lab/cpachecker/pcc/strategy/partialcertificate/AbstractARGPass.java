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

import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;


public abstract class AbstractARGPass {

  private final boolean visitMultipleTimes;

  public AbstractARGPass(final boolean pMultipleVisits) {
    visitMultipleTimes = pMultipleVisits;
  }

  public void passARG(ARGState root) {
    Set<ARGState> seen = new HashSet<>();
    Deque<ARGState> toVisit = new ArrayDeque<>();
    ARGState currentNode;
    boolean childKnown;

    toVisit.add(root);
    seen.add(root);

    while (!toVisit.isEmpty()) {
      currentNode = toVisit.pollLast();
      visitARGNode(currentNode);

      if (!stopPathDiscovery(currentNode)) {
        for (ARGState child : currentNode.getChildren()) {
          childKnown = seen.contains(child);
          if (!childKnown) {
            toVisit.addLast(child);
            seen.add(child);
          }
          if (visitMultipleTimes && childKnown) {
            visitARGNode(child);
          }
        }
      }
    }
  }

  public abstract void visitARGNode(ARGState node);

  public abstract boolean stopPathDiscovery(ARGState node);
}
