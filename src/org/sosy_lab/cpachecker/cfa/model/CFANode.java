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
package org.sosy_lab.cpachecker.cfa.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getLast;

import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

import java.util.ArrayList;
import java.util.List;

public class CFANode implements Comparable<CFANode> {

  private static final UniqueIdGenerator idGenerator = new UniqueIdGenerator();

  private final int nodeNumber;

  private final List<CFAEdge> leavingEdges = new ArrayList<>(1);
  private final List<CFAEdge> enteringEdges = new ArrayList<>(1);

  // is start node of a loop?
  private boolean isLoopStart = false;

  // in which function is that node?
  private final String functionName;

  // list of summary edges
  private FunctionSummaryEdge leavingSummaryEdge = null;
  private FunctionSummaryEdge enteringSummaryEdge = null;

  // reverse postorder sort id, smaller if it appears later in sorting
  private int reversePostorderId = 0;
  // post dominator order, x post-dominate y <=> f(x) <= f(y)
  private int postDominatorId = 0;

  public CFANode(String pFunctionName) {
    assert !pFunctionName.isEmpty();

    functionName = pFunctionName;
    nodeNumber = idGenerator.getFreshId();
  }

  public int getNodeNumber() {
    return nodeNumber;
  }

  public int getReversePostorderId() {
    return reversePostorderId;
  }

  public void setReversePostorderId(int pId) {
    reversePostorderId = pId;
  }

  public int getPostDominatorId() {
    return postDominatorId;
  }

  public void setPostDominatorId(int pId) {
    postDominatorId = pId;
  }

  public void addLeavingEdge(CFAEdge pNewLeavingEdge) {
    checkArgument(pNewLeavingEdge.getPredecessor() == this,
        "Cannot add edge \"%s\" to node %s as leaving edge", pNewLeavingEdge, this);
    leavingEdges.add(pNewLeavingEdge);
  }

  public void removeLeavingEdge(CFAEdge pEdge) {
    boolean removed = leavingEdges.remove(pEdge);
    checkArgument(removed,
        "Cannot remove non-existing leaving edge \"%s\" from node %s", pEdge, this);
  }

  public int getNumLeavingEdges() {
    return leavingEdges.size();
  }

  public CFAEdge getLeavingEdge(int pIndex) {
    return leavingEdges.get(pIndex);
  }

  public void addEnteringEdge(CFAEdge pEnteringEdge) {
    checkArgument(pEnteringEdge.getSuccessor() == this,
        "Cannot add edge \"%s\" to node %s as entering edge", pEnteringEdge, this);
    enteringEdges.add(pEnteringEdge);
  }

  public void removeEnteringEdge(CFAEdge pEdge) {
    boolean removed = enteringEdges.remove(pEdge);
    checkArgument(removed,
        "Cannot remove non-existing entering edge \"%s\" from node %s", pEdge, this);
  }

  public int getNumEnteringEdges() {
    return enteringEdges.size();
  }

  public CFAEdge getEnteringEdge(int pIndex) {
    return enteringEdges.get(pIndex);
  }

  public CFAEdge getEdgeTo(CFANode pOther) {
    for (CFAEdge edge : leavingEdges) {
      if (edge.getSuccessor() == pOther) {
        return edge;
      }
    }

    throw new IllegalArgumentException("there is no edge from " + this + " to " + pOther);
  }

  public boolean hasEdgeTo(CFANode pOther) {
    boolean hasEdge = false;
    for (CFAEdge edge : leavingEdges) {
      if (edge.getSuccessor() == pOther) {
        hasEdge = true;
        break;
      }
    }

    return hasEdge;
  }

  public void setLoopStart() {
    isLoopStart = true;
  }

  public boolean isLoopStart() {
    return isLoopStart;
  }

  public String getFunctionName() {
    return functionName;
  }

  public void addEnteringSummaryEdge(FunctionSummaryEdge pEdge) {
    checkState(enteringSummaryEdge == null,
        "Cannot add two entering summary edges to node %s", this);
    enteringSummaryEdge = pEdge;
  }

  public void addLeavingSummaryEdge(FunctionSummaryEdge pEdge) {
    checkState(leavingSummaryEdge == null,
        "Cannot add two leaving summary edges to node %s", this);
    leavingSummaryEdge = pEdge;
  }

  public FunctionSummaryEdge getEnteringSummaryEdge() {
    return enteringSummaryEdge;
  }

  public FunctionSummaryEdge getLeavingSummaryEdge() {
    return leavingSummaryEdge;
  }

  public void removeEnteringSummaryEdge(FunctionSummaryEdge pEdge) {
    checkArgument(enteringSummaryEdge == pEdge,
        "Cannot remove non-existing entering summary edge \"%s\" from node \"%s\"", pEdge, this);
    enteringSummaryEdge = null;
  }

  public void removeLeavingSummaryEdge(FunctionSummaryEdge pEdge) {
    checkArgument(leavingSummaryEdge == pEdge,
        "Cannot remove non-existing leaving summary edge \"%s\" from node \"%s\"", pEdge, this);
    leavingSummaryEdge = null;
  }

  @Override
  public String toString() {
    return "N" + nodeNumber;
  }

  @Override
  public final int compareTo(CFANode pOther) {
    return Integer.compare(this.nodeNumber, pOther.nodeNumber);
  }

  @Override
  public final boolean equals(Object pObj) {
    // Object.equals() is consistent with our compareTo()
    // because nodeNumber is a unique identifier.
    return super.equals(pObj);
  }

  @Override
  public final int hashCode() {
    // Object.hashCode() is consistent with our compareTo()
    // because nodeNumber is a unique identifier.
    return super.hashCode();
  }

  /**
   * Return a human-readable string describing to which point in the program
   * this state belongs to.
   * Returns the empty string if no suitable description can be found.
   *
   * Normally CFANodes do not belong to a file location,
   * so this should be used only as a best-effort guess to give a user
   * at least something to hold on.
   * Whenever possible, use the file locations of edges instead.
   */
  public String describeFileLocation() {
    if (this instanceof FunctionEntryNode) {
      return "entry of function " + getFunctionName()
          + " in " + ((FunctionEntryNode) this).getFileLocation();
    }

    if (this instanceof FunctionExitNode) {
      // these nodes do not belong to a location
      return "exit of function " + getFunctionName()
          + " in " + ((FunctionExitNode) this).getEntryNode().getFileLocation();
    }

    if (getNumLeavingEdges() > 0) {
      CFAEdge edge = getLeavingEdge(0);
      if (edge instanceof MultiEdge) {
        edge = ((MultiEdge) edge).getEdges().get(0);
      }
      if (!edge.getFileLocation().equals(FileLocation.DUMMY)) {
        return "before " + edge.getFileLocation();
      }
    }

    if (getNumEnteringEdges() > 0) {
      CFAEdge edge = getEnteringEdge(0);
      if (edge instanceof MultiEdge) {
        edge = getLast(((MultiEdge) edge).getEdges());
      }
      if (!edge.getFileLocation().equals(FileLocation.DUMMY)) {
        return "after " + edge.getFileLocation();
      }
    }

    return "";
  }
}
