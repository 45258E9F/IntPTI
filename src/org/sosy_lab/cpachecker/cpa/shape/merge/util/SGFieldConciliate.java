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
package org.sosy_lab.cpachecker.cpa.shape.merge.util;

import org.sosy_lab.cpachecker.cpa.shape.graphs.CShapeGraph;
import org.sosy_lab.cpachecker.cpa.shape.graphs.ShapeGraph;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdge;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdgeFilter;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.util.SymbolicValueFactory;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

public class SGFieldConciliate {

  private CShapeGraph pSG1;
  private CShapeGraph pSG2;

  /**
   * Conciliate has-value edges on pObject1 and pObject2 with respect to pG1 and pG2.
   * Precondition: two objects should be both zero-initialized or not.
   * Contract: the existing has-value edge should not be overwritten
   * MODIFIED: pG1, pG2
   */
  public SGFieldConciliate(CShapeGraph pG1, CShapeGraph pG2, SGObject pObject1, SGObject pObject2) {
    if (!(pG1.getObjects().contains(pObject1)) || !(pG2.getObjects().contains(pObject2))) {
      throw new IllegalArgumentException("object(s) not in the given shape graph(s)");
    }
    // the original shape graph should not be directly changed
    pSG1 = new CShapeGraph(pG1);
    pSG2 = new CShapeGraph(pG2);
    boolean zeroInit = pObject1.isZeroInit();

    // STEP 1: add native non-null edges and common null edges
    Set<SGHasValueEdge> newHasValueEdgeSet1 = new HashSet<>();
    Set<SGHasValueEdge> newHasValueEdgeSet2 = new HashSet<>();
    getNativeCommonHasValueEdges(pG1, pG2, pObject1, pObject2, newHasValueEdgeSet1,
        newHasValueEdgeSet2, zeroInit);
    pSG1.replaceHVSet(newHasValueEdgeSet1);
    pSG2.replaceHVSet(newHasValueEdgeSet2);

    // STEP 2: add missing null edges
    Set<SGHasValueEdge> missingHasValueSet1 = new HashSet<>();
    Set<SGHasValueEdge> missingHasValueSet2 = new HashSet<>();
    getMissingHasValueEdges(pG1, pG2, pObject1, pObject2, missingHasValueSet1,
        missingHasValueSet2, zeroInit);
    newHasValueEdgeSet1.addAll(missingHasValueSet1);
    newHasValueEdgeSet2.addAll(missingHasValueSet2);
    pSG1.replaceHVSet(newHasValueEdgeSet1);
    pSG2.replaceHVSet(newHasValueEdgeSet2);

    // STEP 3: traverse other non-null has-value edges and create new edges with fresh values
    Set<SGHasValueEdge> extHasValueSet1 = mergeNonNullHasValueEdges(pG1, pG2, pObject1, pObject2);
    Set<SGHasValueEdge> extHasValueSet2 = mergeNonNullHasValueEdges(pG2, pG1, pObject2, pObject1);
    newHasValueEdgeSet1.addAll(extHasValueSet1);
    newHasValueEdgeSet2.addAll(extHasValueSet2);
    pSG1.replaceHVSet(newHasValueEdgeSet1);
    pSG2.replaceHVSet(newHasValueEdgeSet2);
  }

  /* *********** */
  /* write backs */
  /* *********** */

  public CShapeGraph getShapeGraph1() {
    return pSG1;
  }

  public CShapeGraph getShapeGraph2() {
    return pSG2;
  }

  /* ***************** */
  /* auxiliary methods */
  /* ***************** */

  /**
   * Find:
   * (1) native non-null has-value edges in pObject1 (OR pObject2),
   * (2) common null has-value edges shared by pObject1 and pObject2.
   */
  private void getNativeCommonHasValueEdges(
      CShapeGraph pG1, CShapeGraph pG2,
      SGObject pObject1, SGObject pObject2,
      Set<SGHasValueEdge> new1, Set<SGHasValueEdge> new2,
      boolean zeroInit) {
    new1.addAll(pG1.getHVEdges());
    SGHasValueEdgeFilter nullFilter1 = SGHasValueEdgeFilter.objectFilter(pObject1)
        .filterHavingValue(ShapeGraph.getNullAddress());
    new1.removeAll(pG1.getHVEdges(nullFilter1));
    new2.addAll(pG2.getHVEdges());
    SGHasValueEdgeFilter nullFilter2 = SGHasValueEdgeFilter.objectFilter(pObject2)
        .filterHavingValue(ShapeGraph.getNullAddress());
    new2.removeAll(pG2.getHVEdges(nullFilter2));
    BitSet nullBytes;
    if (zeroInit) {
      nullBytes = pG1.getNonNullBytesFor(pObject1);
      nullBytes.or(pG2.getNonNullBytesFor(pObject2));
      nullBytes.flip(0, nullBytes.length());
    } else {
      nullBytes = pG1.getNullBytesFor(pObject1);
      nullBytes.and(pG2.getNullBytesFor(pObject2));
    }
    int size = 0;
    for (int i = nullBytes.nextSetBit(0); i >= 0; i = nullBytes.nextSetBit(i + 1)) {
      size++;
      if (size > 0 && (!nullBytes.get(i + 1) || i + 1 == nullBytes.length())) {
        new1.add(new SGHasValueEdge(size, i - (size - 1), pObject1, ShapeGraph.getNullAddress()));
        new2.add(new SGHasValueEdge(size, i - (size - 1), pObject2, ShapeGraph.getNullAddress()));
        size = 0;
      }
    }
  }

  /**
   * Find nullified segments in pObject1 (OR pObject2) but not shared by two objects.
   */
  private void getMissingHasValueEdges(
      CShapeGraph pG1, CShapeGraph pG2,
      SGObject pObject1, SGObject pObject2,
      Set<SGHasValueEdge> missing1, Set<SGHasValueEdge> missing2,
      boolean isZeroInit) {
    BitSet xorBytes, bytes1;
    if (isZeroInit) {
      bytes1 = pG1.getNonNullBytesFor(pObject1);
      xorBytes = pG2.getNonNullBytesFor(pObject2);
      xorBytes.xor(bytes1);
    } else {
      bytes1 = pG1.getNullBytesFor(pObject1);
      xorBytes = pG2.getNullBytesFor(pObject2);
      xorBytes.xor(bytes1);
    }
    // now the segments being set contains all the bytes that are nullified in object1 but not in
    // object2, or vice versa
    SGHasValueEdgeFilter filter1 = SGHasValueEdgeFilter.objectFilter(pObject1);
    SGHasValueEdgeFilter filter2 = SGHasValueEdgeFilter.objectFilter(pObject2);
    int size = 0;
    for (int i = xorBytes.nextSetBit(0); i >= 0; i = xorBytes.nextSetBit(i + 1)) {
      size++;
      if (size > 0 && (!xorBytes.get(i + 1) || i + 1 == xorBytes.length())) {
        int offset = i - (size - 1);
        if (bytes1.get(i) != isZeroInit) {
          // the segment in the object1 is nullified
          // UPDATE: we should specify the correct type here
          filter1 = filter1.filterAtOffset(offset);
          SGHasValueEdge newEdge1 = findHasValueEdgesForSize(pG1, filter1, size);
          if (newEdge1 == null) {
            newEdge1 = new SGHasValueEdge(size, offset, pObject1, ShapeGraph.getNullAddress());
          }
          missing1.add(newEdge1);
          // we can either insert a fresh value or do nothing. The choice depends on whether the
          // matching segment in the object2 corresponds to a complete has-value edge.
          filter2 = filter2.filterAtOffset(offset);
          if (findHasValueEdgesForSize(pG2, filter2, size) == null) {
            SGHasValueEdge newEdge2 = new SGHasValueEdge(size, offset, pObject2,
                SymbolicValueFactory.getNewValue());
            missing2.add(newEdge2);
          }
        } else {
          // vice versa
          filter2 = filter2.filterAtOffset(offset);
          SGHasValueEdge newEdge2 = findHasValueEdgesForSize(pG2, filter2, size);
          if (newEdge2 == null) {
            newEdge2 = new SGHasValueEdge(size, offset, pObject2, ShapeGraph.getNullAddress());
          }
          missing2.add(newEdge2);
          filter1 = filter1.filterAtOffset(offset);
          if (findHasValueEdgesForSize(pG1, filter1, size) == null) {
            SGHasValueEdge newEdge1 = new SGHasValueEdge(size, offset, pObject1,
                SymbolicValueFactory.getNewValue());
            missing1.add(newEdge1);
          }
        }
        size = 0;
      }
    }
  }

  /**
   * Add fresh has-value edges in object1 in shape-graph1 (with respect to the has-value edges in
   * object2 in shape-graph2) for merging.
   */
  private Set<SGHasValueEdge> mergeNonNullHasValueEdges(
      CShapeGraph pG1, CShapeGraph pG2,
      SGObject pObject1, SGObject pObject2) {
    Set<SGHasValueEdge> edgeSet = new HashSet<>();
    SGHasValueEdgeFilter nonNullFilter2 = SGHasValueEdgeFilter.objectFilter(pObject2)
        .filterNotHavingValue(ShapeGraph.getNullAddress());
    SGHasValueEdgeFilter filter1 = SGHasValueEdgeFilter.objectFilter(pObject1);
    for (SGHasValueEdge edge : pG2.getHVEdges(nonNullFilter2)) {
      filter1 = filter1.filterAtOffset(edge.getOffset()).filterByType(edge.getType());
      if (pG1.getHVEdges(filter1).isEmpty()) {
        // we should create a matching fresh has-value edge here
        SGHasValueEdge newEdge = new SGHasValueEdge(edge.getType(), edge.getOffset(), pObject1,
            SymbolicValueFactory.getNewValue());
        edgeSet.add(newEdge);
      }
    }
    return Collections.unmodifiableSet(edgeSet);
  }

  @Nullable
  private SGHasValueEdge findHasValueEdgesForSize(
      CShapeGraph pG, SGHasValueEdgeFilter pFilter,
      int pSize) {
    Set<SGHasValueEdge> edges = pG.getHVEdges(pFilter);
    for (SGHasValueEdge edge : edges) {
      if (edge.getSizeInBytes(pG.getMachineModel()) == pSize) {
        return edge;
      }
    }
    return null;
  }

}
