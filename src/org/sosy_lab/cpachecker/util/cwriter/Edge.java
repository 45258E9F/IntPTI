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
package org.sosy_lab.cpachecker.util.cwriter;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import java.util.Stack;

class Edge implements Comparable<Edge> {

  private final ARGState childState;
  private final CFAEdge edge;
  private final Stack<FunctionBody> stack;

  public Edge(
      ARGState pChildElement,
      CFAEdge pEdge, Stack<FunctionBody> pStack) {
    childState = pChildElement;
    edge = pEdge;
    stack = pStack;
  }

  public ARGState getChildState() {
    return childState;
  }

  public CFAEdge getEdge() {
    return edge;
  }

  public Stack<FunctionBody> getStack() {
    return stack;
  }

  @Override
  /** comparison based on the child element*/
  public int compareTo(Edge pO) {
    int otherElementId = pO.getChildState().getStateId();
    int thisElementId = this.getChildState().getStateId();

    if (thisElementId > otherElementId) {
      return 1;
    } else if (thisElementId < otherElementId) {
      return -1;
    }
    return 0;
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    } else if (pObj instanceof Edge) {
      int otherElementId = ((Edge) pObj).getChildState().getStateId();
      int thisElementId = this.getChildState().getStateId();
      return thisElementId == otherElementId;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return getChildState().getStateId();
  }
}
