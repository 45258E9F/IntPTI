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
package org.sosy_lab.cpachecker.cpa.shape.merge.joiner;

import com.google.common.collect.Iterables;

import org.sosy_lab.cpachecker.cpa.shape.graphs.CShapeGraph;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdge;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdgeFilter;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.merge.abs.GuardedAbstraction;
import org.sosy_lab.cpachecker.cpa.shape.merge.util.MergeTable;
import org.sosy_lab.cpachecker.cpa.shape.merge.util.SGFieldConciliate;
import org.sosy_lab.cpachecker.cpa.shape.merge.util.SGNodeMapping;

import java.util.Set;

public class SGObjectJoin {

  /**
   * By default, the object join is undefined (i.e. infeasible). Thus, when objects or values
   * cannot be merged are encountered, we can simply stop the join operation.
   */
  private boolean isDefined = false;

  private CShapeGraph inputGraph1;
  private CShapeGraph inputGraph2;
  private CShapeGraph destGraph;

  private SGNodeMapping mapping1;
  private SGNodeMapping mapping2;

  private GuardedAbstraction absInfo;

  /**
   * Join two shape objects into the specified destination.
   * Preconditions: input objects and destination are non-null.
   * MODIFIED: pG1, pG2, pDG, pMap1, pMap2
   */
  public SGObjectJoin(
      CShapeGraph pG1, CShapeGraph pG2, CShapeGraph pDG,
      SGNodeMapping pMap1, SGNodeMapping pMap2,
      SGObject pObj1, SGObject pObj2, SGObject pDestObj,
      GuardedAbstraction pAbsInfo,
      MergeTable pTable) {
    inputGraph1 = pG1;
    inputGraph2 = pG2;
    destGraph = pDG;
    mapping1 = pMap1;
    mapping2 = pMap2;
    absInfo = pAbsInfo;

    // STEP 1: conciliate has-value edges on two objects
    // UPDATE: in many cases it is unnecessary to perform conciliate. We should pre-check the
    // necessity of conciliation to improve the performance for object join.
    if (checkIfConciliationNeeded(pObj1, pObj2)) {
      SGFieldConciliate conciliate = new SGFieldConciliate(inputGraph1, inputGraph2, pObj1, pObj2);
      fieldConciliationUpdater(conciliate);
    }

    // STEP 2: join the matching has-value edges
    SGHasValueEdgeFilter filterOnG1 = SGHasValueEdgeFilter.objectFilter(pObj1);
    SGHasValueEdgeFilter filterOnG2 = SGHasValueEdgeFilter.objectFilter(pObj2);
    Set<SGHasValueEdge> edgesOnG1 = inputGraph1.getHVEdges(filterOnG1);
    for (SGHasValueEdge edgeOnG1 : edgesOnG1) {
      filterOnG2 = filterOnG2.filterAtOffset(edgeOnG1.getOffset()).filterByType(edgeOnG1.getType());
      SGHasValueEdge edgeOnG2 = Iterables.getOnlyElement(inputGraph2.getHVEdges(filterOnG2));
      SGValueJoin valueJoin = new SGValueJoin(inputGraph1, inputGraph2, destGraph,
          mapping1, mapping2,
          edgeOnG1, edgeOnG2,
          absInfo,
          pTable);
      if (!valueJoin.isDefined()) {
        return;
      }
      valueMergeUpdater(valueJoin);
      Long newValue = valueJoin.getValue();
      SGHasValueEdge newEdge;
      if (edgeOnG1.getObject().equals(pDestObj) && newValue.equals(edgeOnG1.getValue())) {
        newEdge = edgeOnG1;
      } else {
        newEdge = new SGHasValueEdge(edgeOnG1.getType(), edgeOnG1.getOffset(), pDestObj, newValue);
      }
      destGraph.addHasValueEdge(newEdge);
    }

    // STEP 3: merge meta-info on objects
    // STEP 3.1: validity
    // we have INVALID > VALID, which could introduce some FNs
    boolean validity1 = inputGraph1.isObjectValid(pObj1);
    boolean validity2 = inputGraph2.isObjectValid(pObj2);
    destGraph.setValidity(pDestObj, (validity1 == validity2) && validity1);
    // STEP 3.2: reference count
    // we always use lower reference count, which could introduce some FNs
    long ref1 = inputGraph1.getRef(pObj1);
    long ref2 = inputGraph2.getRef(pObj2);
    destGraph.setRef(pDestObj, (ref1 > ref2) ? ref2 : ref1);

    isDefined = true;
  }

  private boolean checkIfConciliationNeeded(SGObject pObject1, SGObject pObject2) {
    // If has-value edges on two objects match with each other, we say it is unnecessary to
    // perform field conciliate.
    if (!inputGraph1.getObjects().contains(pObject1) ||
        !(inputGraph2.getObjects().contains(pObject2))) {
      throw new IllegalArgumentException("object(s) not in the given shape graph(s)");
    }
    Set<SGHasValueEdge> hvEdgeSet1 = inputGraph1.getHVEdges(SGHasValueEdgeFilter.objectFilter
        (pObject1));
    SGHasValueEdgeFilter filter2 = SGHasValueEdgeFilter.objectFilter(pObject2);
    // the second set of has-value edges is mutable
    int remainSize = inputGraph2.getHVEdges(filter2).size();
    for (SGHasValueEdge hvEdge : hvEdgeSet1) {
      filter2 = filter2.filterAtOffset(hvEdge.getOffset()).filterByType(hvEdge.getType());
      Set<SGHasValueEdge> hvEdges2 = inputGraph2.getHVEdges(filter2);
      if (hvEdges2.size() != 1) {
        return true;
      }
      remainSize--;
    }
    return (remainSize != 0);
  }

  /* *********** */
  /* write-backs */
  /* *********** */

  public CShapeGraph getShapeGraph1() {
    return inputGraph1;
  }

  public CShapeGraph getShapeGraph2() {
    return inputGraph2;
  }

  public CShapeGraph getDestGraph() {
    return destGraph;
  }

  public SGNodeMapping getMapping1() {
    return mapping1;
  }

  public SGNodeMapping getMapping2() {
    return mapping2;
  }

  public GuardedAbstraction getAbstraction() {
    return absInfo;
  }

  public boolean isDefined() {
    return isDefined;
  }

  /* ******** */
  /* updaters */
  /* ******** */

  private void fieldConciliationUpdater(SGFieldConciliate pConciliate) {
    inputGraph1 = pConciliate.getShapeGraph1();
    inputGraph2 = pConciliate.getShapeGraph2();
  }

  private void valueMergeUpdater(SGValueJoin pJoin) {
    inputGraph1 = pJoin.getShapeGraph1();
    inputGraph2 = pJoin.getShapeGraph2();
    destGraph = pJoin.getDestGraph();
    mapping1 = pJoin.getMapping1();
    mapping2 = pJoin.getMapping2();
    absInfo = pJoin.getAbstraction();
  }

}
