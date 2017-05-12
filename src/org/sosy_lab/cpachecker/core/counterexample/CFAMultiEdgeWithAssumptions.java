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
package org.sosy_lab.cpachecker.core.counterexample;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * This class is used if the error path contains multi edges {@link MultiEdge}.
 * Every edge {@link CFAEdge} of a multi edge has its own {@link CFAEdgeWithAssumptions} edge.
 */
public final class CFAMultiEdgeWithAssumptions extends CFAEdgeWithAssumptions
    implements Iterable<CFAEdgeWithAssumptions> {

  private final List<CFAEdgeWithAssumptions> edgesWithAssignment;

  private CFAMultiEdgeWithAssumptions(
      MultiEdge pEdge, Collection<AExpressionStatement> pAssignments,
      List<CFAEdgeWithAssumptions> pEdges, String pComments) {
    super(pEdge, pAssignments, pComments);
    edgesWithAssignment = ImmutableList.copyOf(pEdges);
  }

  @Override
  public Iterator<CFAEdgeWithAssumptions> iterator() {
    return getEdgesWithAssignment().iterator();
  }

  public List<CFAEdgeWithAssumptions> getEdgesWithAssignment() {
    return edgesWithAssignment;
  }

  @Override
  public CFAEdgeWithAssumptions mergeEdge(CFAEdgeWithAssumptions pEdge) {

    // COMMENT: this merge operation just combine expressions in each of edge
    CFAEdgeWithAssumptions multiEdgeMerge = super.mergeEdge(pEdge);

    if (!(pEdge instanceof CFAMultiEdgeWithAssumptions)) {
      return this;
    }

    CFAMultiEdgeWithAssumptions other = (CFAMultiEdgeWithAssumptions) pEdge;

    assert pEdge.getCFAEdge().equals(getCFAEdge());
    assert other.edgesWithAssignment.size() == this.edgesWithAssignment.size();

    List<CFAEdgeWithAssumptions> result = new ArrayList<>();
    Iterator<CFAEdgeWithAssumptions> otherIt = other.iterator();

    for (CFAEdgeWithAssumptions thisEdge : edgesWithAssignment) {
      result.add(thisEdge.mergeEdge(otherIt.next()));
    }


    return valueOf((MultiEdge) pEdge.getCFAEdge(), result, multiEdgeMerge.getExpStmts(),
        multiEdgeMerge.getComment());
  }

  /**
   * Creates a edge that also contains the assumptions of the edges in the multi edge.
   *
   * @param pEdge        The multi edge in the error path.
   * @param pEdges       The edges and assumptions that are contained in the given multi edge.
   * @param pAssumptions The assumptions at the end of the multi edge. Should contain all changed
   *                     variables, for which a concrete value can be found.
   * @param pComments    Additional values that should be shown to the user, and do not need to be
   *                     parsed.
   * @return A edge {@link CFAMultiEdgeWithAssumptions} that contain the assumptions of the edges
   * that are contained in the given multi edge.
   */
  public static CFAMultiEdgeWithAssumptions valueOf(
      MultiEdge pEdge, List<CFAEdgeWithAssumptions> pEdges,
      Collection<AExpressionStatement> pAssumptions, String pComments) {
    return new CFAMultiEdgeWithAssumptions(pEdge, pAssumptions, pEdges, pComments);
  }
}