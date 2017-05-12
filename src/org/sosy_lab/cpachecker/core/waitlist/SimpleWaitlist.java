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
package org.sosy_lab.cpachecker.core.waitlist;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Waitlist implementation that supports either a breadth-first (BFS) or
 * depth-first (DFS) strategy for pop().
 */
public class SimpleWaitlist extends AbstractWaitlist<Deque<AbstractState>> {

  private final TraversalMethod traversal;

  protected SimpleWaitlist(TraversalMethod pTraversal) {
    super(new ArrayDeque<AbstractState>());
    Preconditions
        .checkArgument(pTraversal == TraversalMethod.BFS || pTraversal == TraversalMethod.DFS);
    traversal = pTraversal;
  }

  @Override
  public AbstractState pop() {
    switch (traversal) {
      case BFS:
        return waitlist.removeFirst();

      case DFS:
        return waitlist.removeLast();

      default:
        assert false;
        return null;
    }
  }
}