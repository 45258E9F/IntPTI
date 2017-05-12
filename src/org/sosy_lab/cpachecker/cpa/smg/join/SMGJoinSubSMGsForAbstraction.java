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
package org.sosy_lab.cpachecker.cpa.smg.join;

import com.google.common.collect.Iterables;

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.objects.dls.SMGDoublyLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.objects.dls.SMGDoublyLinkedListCandidate;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;


final public class SMGJoinSubSMGsForAbstraction {

  private SMGJoinStatus status = null;
  private SMG resultSMG = null;
  private SMGObject newAbstractObject = null;
  private Set<Integer> nonSharedValuesFromSMG1 = null;
  private Set<Integer> nonSharedValuesFromSMG2 = null;
  private Set<SMGObject> nonSharedObjectsFromSMG1 = null;
  private Set<SMGObject> nonSharedObjectsFromSMG2 = null;
  private boolean defined = false;

  public SMGJoinSubSMGsForAbstraction(
      SMG inputSMG,
      SMGObject obj1,
      SMGObject obj2,
      SMGDoublyLinkedListCandidate dlsc) throws SMGInconsistentException {

    SMG smg = new SMG(inputSMG);

    SMGEdgeHasValue prevObj1hve = Iterables.getOnlyElement(
        smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(obj1).filterAtOffset(dlsc.getPfo())));
    SMGEdgeHasValue nextObj1hve = Iterables.getOnlyElement(
        smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(obj1).filterAtOffset(dlsc.getNfo())));
    SMGEdgeHasValue prevObj2hve = Iterables.getOnlyElement(
        smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(obj2).filterAtOffset(dlsc.getPfo())));
    SMGEdgeHasValue nextObj2hve = Iterables.getOnlyElement(
        smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(obj2).filterAtOffset(dlsc.getNfo())));

    SMGEdgeHasValue prevObj1hveT =
        new SMGEdgeHasValue(prevObj1hve.getType(), prevObj1hve.getOffset(), prevObj1hve.getObject(),
            0);
    SMGEdgeHasValue nextObj1hveT =
        new SMGEdgeHasValue(nextObj1hve.getType(), nextObj1hve.getOffset(), nextObj1hve.getObject(),
            0);
    SMGEdgeHasValue prevObj2hveT =
        new SMGEdgeHasValue(prevObj2hve.getType(), prevObj2hve.getOffset(), prevObj2hve.getObject(),
            0);
    SMGEdgeHasValue nextObj2hveT =
        new SMGEdgeHasValue(nextObj2hve.getType(), nextObj2hve.getOffset(), nextObj2hve.getObject(),
            0);

    smg.removeHasValueEdge(prevObj1hve);
    smg.removeHasValueEdge(nextObj1hve);
    smg.removeHasValueEdge(prevObj2hve);
    smg.removeHasValueEdge(nextObj2hve);

    smg.addHasValueEdge(prevObj1hveT);
    smg.addHasValueEdge(nextObj1hveT);
    smg.addHasValueEdge(prevObj2hveT);
    smg.addHasValueEdge(nextObj2hveT);

    int lengthObj1 =
        obj1 instanceof SMGDoublyLinkedList ? ((SMGDoublyLinkedList) obj1).getMinimumLength() : 0;
    int lengthObj2 =
        obj2 instanceof SMGDoublyLinkedList ? ((SMGDoublyLinkedList) obj2).getMinimumLength() : 0;

    SMGDoublyLinkedList dls =
        new SMGDoublyLinkedList(obj1.getSize(), dlsc.getHfo(), dlsc.getNfo(), dlsc.getPfo(),
            lengthObj1 + lengthObj2, obj1.getLevel());
    inputSMG.addObject(dls);

    int lDiff;

    if (obj1 instanceof SMGAbstractObject && obj2 instanceof SMGAbstractObject) {
      lDiff = 0;
    } else {
      lDiff = obj1 instanceof SMGAbstractObject ? 1 : -1;
    }

    SMGNodeMapping mapping1 = new SMGNodeMapping();
    SMGNodeMapping mapping2 = new SMGNodeMapping();

    mapping1.map(obj1, dls);
    mapping2.map(obj2, dls);

    boolean increaseLevelAndRelabelTargetSpc =
        obj1 instanceof SMGRegion && obj2 instanceof SMGRegion;

    SMGJoinSubSMGs jss =
        new SMGJoinSubSMGs(SMGJoinStatus.EQUAL, smg, smg, smg, mapping1, mapping2, obj1, obj2, dls,
            lDiff, increaseLevelAndRelabelTargetSpc, true);

    if (!jss.isDefined()) {
      return;
    }

    smg = jss.getDestSMG();
    SMGJoinStatus s = jss.getStatus();
    mapping1 = jss.getMapping1();
    mapping2 = jss.getMapping2();

    //TODO Contains dls0Cycle?

    smg.removeHasValueEdge(prevObj1hveT);
    smg.removeHasValueEdge(nextObj1hveT);
    smg.removeHasValueEdge(prevObj2hveT);
    smg.removeHasValueEdge(nextObj2hveT);

    smg.addHasValueEdge(prevObj1hve);
    smg.addHasValueEdge(nextObj1hve);
    smg.addHasValueEdge(prevObj2hve);
    smg.addHasValueEdge(nextObj2hve);

    defined = true;
    status = s;
    resultSMG = smg;
    newAbstractObject = dls;

    nonSharedObjectsFromSMG1 = new HashSet<>();
    nonSharedObjectsFromSMG2 = new HashSet<>();

    nonSharedValuesFromSMG1 = new HashSet<>();
    nonSharedValuesFromSMG2 = new HashSet<>();

    for (Entry<SMGObject, SMGObject> entry : mapping1.getObject_mapEntrySet()) {
      if (smg.getObjects().contains(entry.getValue())) {
        nonSharedObjectsFromSMG1.add(entry.getValue());
      }
    }

    for (Entry<SMGObject, SMGObject> entry : mapping2.getObject_mapEntrySet()) {
      if (smg.getObjects().contains(entry.getValue())) {
        nonSharedObjectsFromSMG2.add(entry.getValue());
      }
    }

    for (Entry<Integer, Integer> entry : mapping1.getValue_mapEntrySet()) {
      if (smg.getValues().contains(entry.getValue())) {
        nonSharedValuesFromSMG1.add(entry.getValue());
      }
    }

    for (Entry<Integer, Integer> entry : mapping2.getValue_mapEntrySet()) {
      if (smg.getValues().contains(entry.getValue())) {
        nonSharedValuesFromSMG2.add(entry.getValue());
      }
    }
  }

  public boolean isDefined() {
    return defined;
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

  public SMG getResultSMG() {
    return resultSMG;
  }

  public SMGObject getNewAbstractObject() {
    return newAbstractObject;
  }

  public Set<SMGObject> getNonSharedObjectsFromSMG2() {
    return nonSharedObjectsFromSMG2;
  }

  public Set<SMGObject> getNonSharedObjectsFromSMG1() {
    return nonSharedObjectsFromSMG1;
  }

  public Set<Integer> getNonSharedValuesFromSMG1() {
    return nonSharedValuesFromSMG1;
  }

  public Set<Integer> getNonSharedValuesFromSMG2() {
    return nonSharedValuesFromSMG2;
  }
}