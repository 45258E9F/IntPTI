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
package org.sosy_lab.cpachecker.cpa.smg.join;

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.dls.SMGDoublyLinkedList;

import java.util.Collection;

final class SMGJoinMapTargetAddress {
  private SMG smg;
  private SMGNodeMapping mapping1;
  private SMGNodeMapping mapping2;
  private Integer value;

  /**
   * Merge address values. New address value should be the address value of merged target object
   * in merged SMG.
   * Precondition: target objects have been already merged.
   */
  public SMGJoinMapTargetAddress(
      SMG pSMG1, SMG destSMG, SMGNodeMapping pMapping1,
      SMGNodeMapping pMapping2, Integer pAddress1,
      Integer pAddress2, boolean relabel) {
    smg = destSMG;
    mapping1 = pMapping1;
    mapping2 = pMapping2;
    SMGObject target = destSMG.getNullObject();

    // TODO: Ugly, refactor
    SMGEdgePointsTo pt = pSMG1.getPointer(pAddress1);
    SMGEdgePointsTo pt2 = pSMG1.getPointer(pAddress2);
    if (pt.getObject().notNull()) {
      target = pMapping1.get(pt.getObject());
    }

    SMGTargetSpecifier tg;

    if (pt.getObject() instanceof SMGDoublyLinkedList || pt2 == null) {
      tg = pt.getTargetSpecifier();
    } else {
      tg = pt2.getTargetSpecifier();
    }

    // TODO: Ugly, refactor
    Collection<SMGEdgePointsTo> edges = smg.getPTEdges().values();
    for (SMGEdgePointsTo edge : edges) {
      if ((edge.getObject() == target) &&
          (edge.getOffset() == pt.getOffset())) {
        value = edge.getValue();
        return;
      }
    }

    // we did not find the merged pt-edge
    // since pAddress1 and pAddress2 point to the same object in merging, if they are not equal,
    // we should create a fresh symbolic value as the address of merged object
    if (pAddress1.equals(pAddress2)) {
      value = pAddress1;
    } else {
      value = SMGValueFactory.getNewValue();
    }

    smg.addValue(value);

    if (relabel && target instanceof SMGDoublyLinkedList) {
      tg = SMGTargetSpecifier.ALL;
    }

    SMGEdgePointsTo nPtEdge = new SMGEdgePointsTo(value, target, pt.getOffset(), tg);

    smg.addPointsToEdge(nPtEdge);
    mapping1.map(pAddress1, value);
    mapping2.map(pAddress2, value);
  }

  public SMG getSMG() {
    return smg;
  }

  public SMGNodeMapping getMapping1() {
    return mapping1;
  }

  public SMGNodeMapping getMapping2() {
    return mapping2;
  }

  public Integer getValue() {
    return value;
  }
}