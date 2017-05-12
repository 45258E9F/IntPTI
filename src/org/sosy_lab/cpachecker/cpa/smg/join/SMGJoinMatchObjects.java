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

import com.google.common.collect.Iterators;

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.dls.SMGDoublyLinkedList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class SMGJoinMatchObjects {
  private boolean defined = false;
  private SMGJoinStatus status;

  private static boolean checkNull(SMGObject pObj1, SMGObject pObj2) {
    if (pObj1.notNull() && pObj2.notNull()) {
      return false;
    }

    return true;
  }

  private static boolean checkMatchingMapping(
      SMGObject pObj1, SMGObject pObj2,
      SMGNodeMapping pMapping1, SMGNodeMapping pMapping2) {
    if (pMapping1.containsKey(pObj1) && pMapping2.containsKey(pObj2) &&
        pMapping1.get(pObj1) != pMapping2.get(pObj2)) {
      return true;
    }

    return false;
  }

  private static boolean checkConsistentMapping(
      SMGObject pObj1, SMGObject pObj2,
      SMGNodeMapping pMapping1, SMGNodeMapping pMapping2) {
    if ((pMapping1.containsKey(pObj1) && pMapping2.containsValue(pMapping1.get(pObj1))) ||
        (pMapping2.containsKey(pObj2) && pMapping1.containsValue(pMapping2.get(pObj2)))) {
      return true;
    }

    return false;
  }

  private static boolean checkConsistentObjects(
      SMGObject pObj1, SMGObject pObj2,
      SMG pSMG1, SMG pSMG2) {
    if ((pObj1.getSize() != pObj2.getSize()) ||
        (pSMG1.isObjectValid(pObj1) != pSMG2.isObjectValid(pObj2))) {
      return true;
    }

    return false;
  }

  /**
   * Two objects have inconsistent fields only if there exists a pair of matching has-value edges
   * such that their values are mapped to different merged values.
   */
  private static boolean checkConsistentFields(
      SMGObject pObj1, SMGObject pObj2,
      SMGNodeMapping pMapping1, SMGNodeMapping pMapping2,
      SMG pSMG1, SMG pSMG2) {

    List<SMGEdgeHasValue> fields = new ArrayList<>();

    fields.addAll(pSMG1.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObj1)));
    fields.addAll(pSMG2.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObj2)));

    //TODO: We go through some fields twice, fix
    for (SMGEdgeHasValue hv : fields) {
      Set<SMGEdgeHasValue> hv1 = pSMG1.getHVEdges(
          SMGEdgeHasValueFilter.objectFilter(pObj1).filterByType(hv.getType())
              .filterAtOffset(hv.getOffset()));
      Set<SMGEdgeHasValue> hv2 = pSMG2.getHVEdges(
          SMGEdgeHasValueFilter.objectFilter(pObj2).filterByType(hv.getType())
              .filterAtOffset(hv.getOffset()));
      if (hv1.size() > 0 && hv2.size() > 0) {
        Integer v1 = Iterators.getOnlyElement(hv1.iterator()).getValue();
        Integer v2 = Iterators.getOnlyElement(hv2.iterator()).getValue();
        if (pMapping1.containsKey(v1) && pMapping2.containsKey(v2) && !(pMapping1.get(v1)
            .equals(pMapping2.get(v2)))) {
          return true;
        }
      }
    }

    return false;
  }

  private static boolean checkMatchingAbstractions(SMGObject pObj1, SMGObject pObj2) {
    // abstraction does not apply for SMGRegion
    if (pObj1.isAbstract() && pObj2.isAbstract()) {
      SMGAbstractObject pAbstract1 = (SMGAbstractObject) pObj1;
      SMGAbstractObject pAbstract2 = (SMGAbstractObject) pObj2;

      //TODO: It should be possible to join some of the different generic shapes, i.e. a SLL
      //      might be a more general segment than a DLL
      if (!(pAbstract1.matchGenericShape(pAbstract2) && pAbstract1
          .matchSpecificShape(pAbstract2))) {
        return true;
      }
    }

    return false;
  }

  /**
   * Precondition: pObj1 and pObj2 have not been merged yet.
   * Check if pObj1 and pObj2 are possibly merged.
   */
  public SMGJoinMatchObjects(
      SMGJoinStatus pStatus, SMG pSMG1, SMG pSMG2,
      SMGNodeMapping pMapping1, SMGNodeMapping pMapping2,
      SMGObject pObj1, SMGObject pObj2) {
    if ((!pSMG1.getObjects().contains(pObj1)) || (!pSMG2.getObjects().contains(pObj2))) {
      throw new IllegalArgumentException();
    }

    // objects should both null or non-null
    if (SMGJoinMatchObjects.checkNull(pObj1, pObj2)) {
      return;
    }

    // objects should be merged consistently in SMG node mapping
    if (SMGJoinMatchObjects.checkMatchingMapping(pObj1, pObj2, pMapping1, pMapping2)) {
      return;
    }

    // pObj1 is mapped to O1 and one object is mapped to O1 in pMapping 2 now, then merge is
    // unfeasible, because pObj2 is impossible to be mapped to O1 by precondition
    if (SMGJoinMatchObjects.checkConsistentMapping(pObj1, pObj2, pMapping1, pMapping2)) {
      return;
    }

    // two objects should have the same size and validity
    if (SMGJoinMatchObjects.checkConsistentObjects(pObj1, pObj2, pSMG1, pSMG2)) {
      return;
    }

    if (pObj1 instanceof SMGDoublyLinkedList && pObj2 instanceof SMGDoublyLinkedList) {

      SMGDoublyLinkedList l1 = (SMGDoublyLinkedList) pObj1;
      SMGDoublyLinkedList l2 = (SMGDoublyLinkedList) pObj2;

      if (l1.getHfo() != l2.getHfo() || l1.getNfo() != l2.getNfo()
          || l1.getPfo() != l2.getPfo()) {
        return;
      }
    }


    if (SMGJoinMatchObjects.checkMatchingAbstractions(pObj1, pObj2)) {
      return;
    }

    if (SMGJoinMatchObjects
        .checkConsistentFields(pObj1, pObj2, pMapping1, pMapping2, pSMG1, pSMG2)) {
      return;
    }

    status = SMGJoinMatchObjects.updateStatusForAbstractions(pObj1, pObj2, pStatus);
    defined = true;
  }

  private static SMGJoinStatus updateStatusForAbstractions(
      SMGObject pObj1,
      SMGObject pObj2,
      SMGJoinStatus pStatus) {
    // isMoreGeneral() only applies for DLS, SLS, etc.
    if (pObj1.isMoreGeneral(pObj2)) {
      return SMGJoinStatus.updateStatus(pStatus, SMGJoinStatus.LEFT_ENTAIL);
    } else if (pObj2.isMoreGeneral(pObj1)) {
      return SMGJoinStatus.updateStatus(pStatus, SMGJoinStatus.RIGHT_ENTAIL);
    }
    return pStatus;
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

  public boolean isDefined() {
    return defined;
  }
}