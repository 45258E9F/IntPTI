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
package org.sosy_lab.cpachecker.util.collections.tree;


import java.util.Stack;

public final class LeafCountVisitor<S, E> implements TreeVisitor<S, E> {

  private long count = 0;

  public long getLeafCount() {
    return count;
  }

  @Override
  public TreeVisitStrategy visit(Stack<S> path, E element, boolean isLeaf) {
    if (isLeaf) {
      count++;
    }
    return TreeVisitStrategy.CONTINUE;
  }
}
