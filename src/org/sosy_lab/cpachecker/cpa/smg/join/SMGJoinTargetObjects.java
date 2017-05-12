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

import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGGenericAbstractionCandidate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class SMGJoinTargetObjects {
  private SMGJoinStatus status;
  private boolean defined = false;
  private boolean recoverable = false;
  private SMG inputSMG1;
  private SMG inputSMG2;
  private SMG destSMG;
  private Integer value;
  private SMGNodeMapping mapping1;
  private SMGNodeMapping mapping2;

  private List<SMGGenericAbstractionCandidate> abstractionCandidates;

  /**
   * Return TRUE if two PT-edges have different offset.
   * If TRUE, the merge is undefined.
   */
  private static boolean matchOffsets(
      SMGJoinTargetObjects pJto,
      SMGEdgePointsTo pt1,
      SMGEdgePointsTo pt2) {
    if (pt1.getOffset() != pt2.getOffset()) {
      pJto.defined = false;
      pJto.recoverable = true;
      return true;
    }

    return false;
  }

  private static boolean checkAlreadyJoined(
      SMGJoinTargetObjects pJto, SMGObject pObj1, SMGObject pObj2,
      Integer pAddress1, Integer pAddress2, boolean pRelabel) {
    if ((!pObj1.notNull()) && (!pObj2.notNull()) ||
        (pJto.mapping1.containsKey(pObj1) && pJto.mapping2.containsKey(pObj2)
            && pJto.mapping1.get(pObj1) == pJto.mapping2.get(pObj2))) {
      // two objects have been merged, then we need to merge address values
      SMGJoinMapTargetAddress mta =
          new SMGJoinMapTargetAddress(pJto.inputSMG1, pJto.destSMG, pJto.mapping1,
              pJto.mapping2, pAddress1,
              pAddress2, pRelabel);
      pJto.defined = true;
      pJto.destSMG = mta.getSMG();
      pJto.mapping1 = mta.getMapping1();
      pJto.mapping2 = mta.getMapping2();
      pJto.value = mta.getValue();
      return true;
    }

    return false;
  }

  /**
   * Return TRUE only if pObj1 and pObj2 are impossible to be merged.
   */
  private static boolean checkObjectMatch(
      SMGJoinTargetObjects pJto,
      SMGObject pObj1,
      SMGObject pObj2) {
    // check if it is feasible to merge pObj1 and pObj2
    SMGJoinMatchObjects mo =
        new SMGJoinMatchObjects(pJto.status, pJto.inputSMG1, pJto.inputSMG2, pJto.mapping1,
            pJto.mapping2, pObj1, pObj2);
    if (!mo.isDefined()) {
      pJto.defined = false;
      pJto.recoverable = true;
      return true;
    }

    pJto.status = mo.getStatus();
    return false;
  }

  /**
   * The interface of this constructor has the same pattern as the one of {@link SMGJoinValues}.
   * The two values to be merged are pointer values.
   */
  public SMGJoinTargetObjects(
      SMGJoinStatus pStatus,
      SMG pSMG1,
      SMG pSMG2,
      SMG pDestSMG,
      SMGNodeMapping pMapping1,
      SMGNodeMapping pMapping2,
      Integer pAddress1,
      Integer pAddress2,
      int pLevel1,
      int pLevel2,
      int ldiff,
      boolean identicalInputSmgs,
      boolean pIncreaseLevelAndRelabel) throws SMGInconsistentException {

    inputSMG1 = pSMG1;
    inputSMG2 = pSMG2;
    mapping1 = pMapping1;
    mapping2 = pMapping2;
    destSMG = pDestSMG;
    status = pStatus;

    SMGEdgePointsTo pt1 = inputSMG1.getPointer(pAddress1);
    SMGEdgePointsTo pt2 = inputSMG2.getPointer(pAddress2);

    if (pLevel1 - pLevel2 != ldiff) {
      defined = false;
      recoverable = true;
      return;
    }

    if (SMGJoinTargetObjects.matchOffsets(this, pt1, pt2)) {
      abstractionCandidates = ImmutableList.of();
      return;
    }

    SMGObject target1 = pt1.getObject();
    SMGObject target2 = pt2.getObject();

    if (SMGJoinTargetObjects.checkAlreadyJoined(this, target1, target2, pAddress1, pAddress2,
        pIncreaseLevelAndRelabel)) {
      abstractionCandidates = ImmutableList.of();
      return;
    }

    // (1) two objects are of different classes (e.g. SMGRegion vs. DLS)
    if (target1.getClass() != target2.getClass() && mapping1.containsKey(pAddress1)
        && mapping2.containsKey(pAddress2)
        && !mapping1.get(target1).equals(mapping2.get(target2))) {
      recoverable = true;
      defined = false;
      return;
    }

    // this only applies for SLS/DLS
    if (target1.getClass() == target2.getClass()
        && pt1.getTargetSpecifier() != pt2.getTargetSpecifier()) {
      recoverable = true;
      defined = false;
      return;
    }

    // check if two objects match (i.e. if it is feasible to merge two objects)
    if (SMGJoinTargetObjects.checkObjectMatch(this, target1, target2)) {
      abstractionCandidates = ImmutableList.of();
      return;
    }

    // two SMGRegions can be joined, only if they have the same size
    SMGObject newObject = target1.join(target2, pIncreaseLevelAndRelabel);
    destSMG.addObject(newObject);

    // two target objects are intended to merged into newObject
    // but we need to remove all existing merged nodes first (WHY???)
    delayedJoin(target1, target2);

    mapping1.map(target1, newObject);
    mapping2.map(target2, newObject);

    // then, we merge address values
    SMGJoinMapTargetAddress mta =
        new SMGJoinMapTargetAddress(inputSMG1, destSMG, mapping1, mapping2, pAddress1, pAddress2,
            pIncreaseLevelAndRelabel);
    destSMG = mta.getSMG();
    mapping1 = mta.getMapping1();
    mapping2 = mta.getMapping2();
    value = mta.getValue();

    // merge target1 and target2 now. We recursively call SMGJoinSubSMGs to do so.
    SMGJoinSubSMGs jss = new SMGJoinSubSMGs(status, inputSMG1, inputSMG2, destSMG,
        mapping1, mapping2,
        target1, target2, newObject, 0, false, identicalInputSmgs);
    if (jss.isDefined()) {
      defined = true;
      status = jss.getStatus();
      abstractionCandidates = jss.getSubSmgAbstractionCandidates();
    }

    abstractionCandidates = ImmutableList.of();
  }

  private void delayedJoin(SMGObject pTarget1, SMGObject pTarget2) {

    // pTarget1 has been merged with one object
    // we remove all the objects and values that are reachable from this merged object
    if (mapping1.containsKey(pTarget1)) {
      removeSubSmgAndMappping(mapping1.get(pTarget1));
    }

    if (mapping2.containsKey(pTarget2)) {
      removeSubSmgAndMappping(mapping1.get(pTarget2));
    }
  }

  private void removeSubSmgAndMappping(SMGObject targetObject) {
    Set<SMGObject> toBeChecked = new HashSet<>();
    Set<SMGObject> reached = new HashSet<>();

    toBeChecked.add(targetObject);
    reached.add(targetObject);

    Set<SMGObject> toCheck = new HashSet<>();

    while (!toBeChecked.isEmpty()) {
      toCheck.clear();
      toCheck.addAll(toBeChecked);
      toBeChecked.clear();

      for (SMGObject objToCheck : toCheck) {
        removeObjectAndNodesFromDestSMG(objToCheck, reached, toBeChecked);
      }
    }
  }

  private void removeObjectAndNodesFromDestSMG(
      SMGObject pObjToCheck, Set<SMGObject> pReached,
      Set<SMGObject> pToBeChecked) {

    // remove pObjToCheck in node mapping
    mapping1.removeValue(pObjToCheck);

    // remove values in pObjToCheck
    Set<SMGEdgeHasValue> hves = destSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObjToCheck));

    for (SMGEdgeHasValue hve : hves) {

      Integer val = hve.getValue();

      mapping1.removeValue(val);

      if (destSMG.isPointer(hve.getValue())) {
        SMGObject reachedObject = destSMG.getPointer(hve.getValue()).getObject();
        if (!pReached.contains(reachedObject)) {
          pToBeChecked.add(reachedObject);
          pReached.add(reachedObject);
        }
      }
    }

    // remove pObjToCheck
    destSMG.removeObjectAndEdges(pObjToCheck);
  }

  public boolean isDefined() {
    return defined;
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

  public SMG getInputSMG1() {
    return inputSMG1;
  }

  public SMG getDestinationSMG() {
    return destSMG;
  }

  public SMGNodeMapping getMapping1() {
    return mapping1;
  }

  public Integer getValue() {
    return value;
  }

  public boolean isRecoverable() {
    return recoverable;
  }

  public SMG getInputSMG2() {
    return inputSMG2;
  }

  public SMGNodeMapping getMapping2() {
    return mapping2;
  }

  public List<SMGGenericAbstractionCandidate> getAbstractionCandidates() {
    return abstractionCandidates;
  }
}
