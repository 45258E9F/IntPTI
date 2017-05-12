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
package org.sosy_lab.cpachecker.cpa.shape.merge.joiner;

import org.sosy_lab.cpachecker.cpa.shape.graphs.CShapeGraph;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdge;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGHasValueEdgeFilter;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGPointToEdge;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGRegion;
import org.sosy_lab.cpachecker.cpa.shape.merge.util.SGNodeMapping;

import java.util.ArrayDeque;
import java.util.Queue;

public class SGPendingJoin {

  private CShapeGraph destGraph;
  private SGNodeMapping mapping1;
  private SGNodeMapping mapping2;

  /**
   * Precondition: objects in sPends1 should be in pG1, objects in sPends2 should be in pG2
   * MODIFIED: pDG, pMap1, pMap2
   */
  public SGPendingJoin(
      CShapeGraph pG1, CShapeGraph pG2, CShapeGraph pDG,
      SGNodeMapping pMap1, SGNodeMapping pMap2) {
    destGraph = pDG;
    mapping1 = pMap1;
    mapping2 = pMap2;

    Queue<Long> pending1 = new ArrayDeque<>(mapping1.getPendings());
    Queue<Long> pending2 = new ArrayDeque<>(mapping2.getPendings());

    // STEP 1: add pending objects
    addPendingObjects(pG1, pending1, mapping1);
    addPendingObjects(pG2, pending2, mapping2);
    // STEP 2: reset pending set in two node mappings
    mapping1.resetPending();
    mapping2.resetPending();
  }

  /**
   * Precondition: values in pVs are from pG
   */
  private void addPendingObjects(CShapeGraph pG, Queue<Long> pVs, SGNodeMapping pMap) {
    while (!pVs.isEmpty()) {
      Long address = pVs.poll();
      SGPointToEdge ptEdge = pG.getPointer(address);
      SGObject target = ptEdge.getObject();
      // `address` must be a fresh value for destination shape graph. If not, this value is
      // derived from merging v1 from G1 and v2 from G2. By the monotonicity of symbolic value,
      // the fresh value must be larger than the value of `address`. Thus, (v1, v2) = `address`
      // only if both v1 and v2 have the value of `address`, in which case v1 and v2 are merged
      // normally and removed from the pending set. That was a contradiction.
      Long newAddress = pMap.getReplacement(address);
      if (newAddress != null) {
        // replace the pending address with the new one (we should update the address value and
        // the corresponding point-to edge consistently)
        address = newAddress;
        if (destGraph.isPointer(address)) {
          // the certain point-to edge exists in the merged shape graph now
          continue;
        }
        ptEdge = new SGPointToEdge(address, target, ptEdge.getOffset());
      }
      // If the pending object is already merged, we only need to add a point-to edge and then
      // continue to handle the next pending object.
      if (pMap.containsKey(target)) {
        destGraph.addValue(address);
        destGraph.addPointToEdge(ptEdge);
        continue;
      }
      // If the pending object is not merged yet, it can be heap or global object (i.e. string
      // literal)
      if (pG.isHeapObject(target)) {
        destGraph.addHeapObject(target, pG.getEdgeForHeapObject(target));
      } else {
        if (target instanceof SGRegion && pG.isGlobalObject(target)) {
          destGraph.addGlobalObject((SGRegion) target);
        } else {
          // we just skip the unexpected case
          continue;
        }
      }
      destGraph.addValue(address);
      destGraph.addPointToEdge(ptEdge);
      // add object mapping entry here
      pMap.put(target, target);
      // traverse has-value edges on this target object
      // It is safe to directly add old values (v) on the target object. If the destination object
      // has already contained v, then v from G1 and v from G2 have been merged.
      // If the value (to be inserted) is a pointer value, we should first check if v exists. If
      // not , we add the pointer value into the pending set.
      SGHasValueEdgeFilter filter = SGHasValueEdgeFilter.objectFilter(target);
      for (SGHasValueEdge hvEdge : pG.getHVEdges(filter)) {
        Long value = hvEdge.getValue();
        if (!destGraph.getValues().contains(value)) {
          destGraph.addValue(value);
          if (pG.isPointer(value) && !value.equals(CShapeGraph.getNullAddress())) {
            pVs.offer(value);
          }
        }
        destGraph.addHasValueEdge(hvEdge);
      }
    }
  }

  /* *********** */
  /* write backs */
  /* *********** */

  public CShapeGraph getDestGraph() {
    return destGraph;
  }

  public SGNodeMapping getMapping1() {
    return mapping1;
  }

  public SGNodeMapping getMapping2() {
    return mapping2;
  }

}
