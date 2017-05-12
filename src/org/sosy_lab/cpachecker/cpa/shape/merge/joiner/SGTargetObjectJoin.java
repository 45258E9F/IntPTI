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

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.shape.graphs.CShapeGraph;
import org.sosy_lab.cpachecker.cpa.shape.graphs.edge.SGPointToEdge;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGAbstract;
import org.sosy_lab.cpachecker.cpa.shape.graphs.node.SGObject;
import org.sosy_lab.cpachecker.cpa.shape.merge.abs.GuardedAbstraction;
import org.sosy_lab.cpachecker.cpa.shape.merge.util.MergeTable;
import org.sosy_lab.cpachecker.cpa.shape.merge.util.SGNodeMapping;
import org.sosy_lab.cpachecker.cpa.shape.util.SymbolicValueFactory;

public class SGTargetObjectJoin {

  private CShapeGraph inputGraph1;
  private CShapeGraph inputGraph2;
  private CShapeGraph destGraph;

  private SGNodeMapping mapping1;
  private SGNodeMapping mapping2;

  private GuardedAbstraction absInfo;

  private boolean isDefined = false;
  private Long merged;

  /**
   * Merge two pointers.
   * MODIFIED: pG1, pG2 (due to the recursive call to SGObjectJoin), pDG, pMap1, pMap2
   */
  public SGTargetObjectJoin(
      CShapeGraph pG1, CShapeGraph pG2, CShapeGraph pDG,
      SGNodeMapping pMap1, SGNodeMapping pMap2,
      Long pAddress1, Long pAddress2,
      GuardedAbstraction pAbsInfo,
      MergeTable pTable) {
    inputGraph1 = pG1;
    inputGraph2 = pG2;
    destGraph = pDG;
    mapping1 = pMap1;
    mapping2 = pMap2;
    absInfo = pAbsInfo;

    SGPointToEdge ptEdge1 = inputGraph1.getPointer(pAddress1);
    SGPointToEdge ptEdge2 = inputGraph2.getPointer(pAddress2);
    assert (ptEdge1 != null && ptEdge2 != null);

    if (!joinPointersWithMatchingOffsets(ptEdge1, ptEdge2)) {
      return;
    }
    // check if the two address values are consistent (i.e. equal in the sense of equivalence
    // relation)
    Long mergedAddress = pTable.merge(pAddress1, pAddress2);
    SGObject target1 = ptEdge1.getObject();
    SGObject target2 = ptEdge2.getObject();
    if (mergedAddress == null) {
      // two address values are inconsistent, then we pend two values
      if (target1.notNull()) {
        mapping1.addPending(pAddress1);
      }
      if (target2.notNull()) {
        mapping2.addPending(pAddress2);
      }
      // create a new value as new address value for this has-value edge
      Long newValue = SGValueJoin.mergeSymbolicValues(pAddress1, pAddress2, inputGraph1,
          inputGraph2, destGraph, absInfo, pTable);
      // add a point-to edge from the new value to the void object, thus the new value is a
      // pointer (which makes the following merges feasible)
      SGPointToEdge voidEdge = new SGPointToEdge(newValue, SGObject.getVoidObject(), 0);
      destGraph.addPointToEdge(voidEdge);
      merged = newValue;
      isDefined = true;
    } else {
      // two address values are consistent
      // STEP 1: check whether two targets have already been merged
      if (joinAlreadyJoinedPointers(target1, target2, pAddress1, pAddress2, pTable)) {
        return;
      }
      if (!checkJoinFeasibility(target1, target2)) {
        return;
      }
      // STEP 2: check the consistency of targets
      SGObject newObject = target1.join(target2);
      merged = mergedAddress;
      if (!(newObject instanceof SGAbstract)) {
        CFAEdge edge = inputGraph1.getEdgeForHeapObject(target1);
        assert (edge != null) : "every heap object should be associated with a CFA edge";
        destGraph.addHeapObject(newObject, edge);
        mapping1.put(target1, newObject);
        mapping2.put(target2, newObject);
        String name1 = target1.getLabel();
        String name2 = target2.getLabel();
        if (!name1.equals(name2)) {
          absInfo.addNameAlias(name1, name2);
        }
        SGAddressValueJoin addressJoin = new SGAddressValueJoin(inputGraph1, inputGraph2,
            destGraph, mapping1, mapping2, pAddress1, pAddress2, absInfo, pTable);
        addressJoinUpdater(addressJoin);
        mapping1.put(pAddress1, mergedAddress);
        mapping2.put(pAddress2, mergedAddress);
        mapping1.removePending(pAddress1);
        mapping2.removePending(pAddress2);
        SGObjectJoin targetJoin = new SGObjectJoin(inputGraph1, inputGraph2, destGraph, mapping1,
            mapping2, target1, target2, newObject, absInfo, pTable);
        if (targetJoin.isDefined()) {
          isDefined = true;
          objectJoinUpdater(targetJoin);
        }
      } else {
        // this case occurs when heap reallocation is performed
        Long nv1 = SymbolicValueFactory.getNewValue();
        Long nv2 = SymbolicValueFactory.getNewValue();
        absInfo.addAbstraction(mergedAddress, inputGraph1, nv1, inputGraph2, nv2);
        destGraph.addValue(mergedAddress);
        SGPointToEdge voidEdge = new SGPointToEdge(mergedAddress, SGObject.getVoidObject(), 0);
        destGraph.addPointToEdge(voidEdge);
        mapping1.addPending(pAddress1, nv1);
        mapping2.addPending(pAddress2, nv2);
        isDefined = true;
      }
    }
  }

  /* ************* */
  /* sanity checks */
  /* ************* */

  private boolean joinPointersWithMatchingOffsets(SGPointToEdge pPT1, SGPointToEdge pPT2) {
    return pPT1.getOffset() == pPT2.getOffset();
  }

  private boolean joinAlreadyJoinedPointers(
      SGObject pTarget1, SGObject pTarget2,
      Long pAddress1, Long pAddress2, MergeTable pTable) {
    // Invariant: each object is allowed to be abstracted into only one object
    boolean isNull1 = !pTarget1.notNull();
    boolean isNull2 = !pTarget2.notNull();
    if (isNull1 && isNull2) {
      merged = CShapeGraph.getNullAddress();
      isDefined = true;
      return true;
    } else if (!isNull1 && !isNull2 &&
        mapping1.containsKey(pTarget1) && mapping2.containsKey(pTarget2) &&
        mapping1.get(pTarget1) == mapping2.get(pTarget2)) {
      // two targets have already been merged
      SGAddressValueJoin addressJoin = new SGAddressValueJoin(inputGraph1, inputGraph2, destGraph,
          mapping1, mapping2,
          pAddress1, pAddress2,
          absInfo,
          pTable);
      isDefined = true;
      merged = addressJoin.getValue();
      addressJoinUpdater(addressJoin);
      return true;
    } else if (isNull1 != isNull2) {
      // join a non-null object with null object
      // NOTE: if we have (O1, O2) = O3, then (O1, NULL) = O3 because NULL can be merged into any
      // object
      SGObject target;
      if (!isNull1) {
        target = mapping1.get(pTarget1);
      } else {
        target = mapping2.get(pTarget2);
      }
      if (target != null) {
        SGAddressValueJoin addressJoin = new SGAddressValueJoin(inputGraph1, inputGraph2, destGraph,
            mapping1, mapping2,
            pAddress1, pAddress2,
            absInfo,
            pTable);
        isDefined = true;
        merged = addressJoin.getValue();
        addressJoinUpdater(addressJoin);
        return true;
      }
    }
    return false;
  }

  private boolean checkJoinFeasibility(SGObject pTarget1, SGObject pTarget2) {
    // two objects should be heap objects, since stack/global objects have been merged in the
    // previous phase
    if (!inputGraph1.isHeapObject(pTarget1) || !inputGraph2.isHeapObject(pTarget2)) {
      return false;
    }
    // it is not allowed that one target is mapped and the other is not
    if ((!pTarget1.notNull() || mapping1.containsKey(pTarget1)) != (!pTarget2.notNull() ||
        mapping2.containsKey(pTarget2))) {
      return false;
    }
    if (mapping1.containsKey(pTarget1) && mapping2.containsKey(pTarget2) && mapping1.get(pTarget1)
        != mapping2.get(pTarget2)) {
      return false;
    }
    // O1 -> O and O' -> O while (O1,O') has not already been merged (otherwise such case can be
    // captured in the previous function)
    // O' is an object other than O2
    if ((mapping1.containsKey(pTarget1) && mapping2.containsValue(mapping1.get(pTarget1))) ||
        (mapping2.containsKey(pTarget2) && mapping1.containsValue(mapping2.get(pTarget2)))) {
      return false;
    }
    return true;
  }

  /* *********** */
  /* write backs */
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

  public Long getValue() {
    return merged;
  }

  /* ******** */
  /* updaters */
  /* ******** */

  private void addressJoinUpdater(SGAddressValueJoin joiner) {
    destGraph = joiner.getDestGraph();
    mapping1 = joiner.getMapping1();
    mapping2 = joiner.getMapping2();
    absInfo = joiner.getAbstraction();
  }

  private void objectJoinUpdater(SGObjectJoin joiner) {
    inputGraph1 = joiner.getShapeGraph1();
    inputGraph2 = joiner.getShapeGraph2();
    destGraph = joiner.getDestGraph();
    mapping1 = joiner.getMapping1();
    mapping2 = joiner.getMapping2();
    absInfo = joiner.getAbstraction();
  }

}
