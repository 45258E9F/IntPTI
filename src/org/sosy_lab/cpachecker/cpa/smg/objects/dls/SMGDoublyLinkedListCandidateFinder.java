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
package org.sosy_lab.cpachecker.cpa.smg.objects.dls;

import com.google.common.collect.Iterables;

import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionFinder;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SMGDoublyLinkedListCandidateFinder implements SMGAbstractionFinder {

  private CLangSMG smg;
  private Map<SMGObject, Map<Pair<Integer, Integer>, SMGDoublyLinkedListCandidate>> candidates =
      new HashMap<>();
  private Map<Integer, Integer> inboundPointers = new HashMap<>();
  private Map<SMGDoublyLinkedListCandidate, Integer> candidateLength = new HashMap<>();

  private final int seqLengthThreshold;

  public SMGDoublyLinkedListCandidateFinder() {
    seqLengthThreshold = 3;
  }

  public SMGDoublyLinkedListCandidateFinder(int pSeqLengthThreshold) {
    seqLengthThreshold = pSeqLengthThreshold;
  }

  public Map<SMGDoublyLinkedListCandidate, Integer> getCandidateLength() {
    return candidateLength;
  }

  @Override
  public Set<SMGAbstractionCandidate> traverse(CLangSMG pSmg) {
    smg = pSmg;

    candidateLength.clear();
    candidates.clear();
    inboundPointers.clear();

    buildInboundPointers();

    for (SMGObject object : smg.getHeapObjects()) {
      startTraversal(object);
    }

    Set<SMGAbstractionCandidate> returnSet = new HashSet<>();
    for (Map<Pair<Integer, Integer>, SMGDoublyLinkedListCandidate> objCandidates : candidates
        .values()) {
      for (SMGDoublyLinkedListCandidate candidate : objCandidates.values()) {
        if (candidateLength.get(candidate) >= seqLengthThreshold) {
          returnSet.add(
              new SMGDoublyLinkedListCandidateSequence(candidate, candidateLength.get(candidate)));
        }
      }
    }
    return Collections.unmodifiableSet(returnSet);
  }

  private void buildInboundPointers() {
    for (Integer pointer : smg.getPTEdges().keySet()) {
      inboundPointers.put(pointer,
          smg.getHVEdges(new SMGEdgeHasValueFilter().filterHavingValue(pointer)).size());
    }
  }

  private void startTraversal(SMGObject pObject) {
    if (candidates.containsKey(pObject)) {
      // Processed already in continueTraversal
      return;
    }
    candidates.put(pObject, new HashMap<Pair<Integer, Integer>, SMGDoublyLinkedListCandidate>());
    createCandidatesOfObject(pObject);
  }

  private void createCandidatesOfObject(SMGObject pObject) {

    if (!smg.isObjectValid(pObject) || !(pObject.getLevel() == 0)) {
      return;
    }

    Set<SMGEdgeHasValue> hvesOfObject = smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObject));

    if (hvesOfObject.size() < 2) {
      return;
    }

    for (SMGEdgeHasValue hveNext : hvesOfObject) {

      int nfo = hveNext.getOffset();
      int nextPointer = hveNext.getValue();

      if (!smg.isPointer(nextPointer)) {
        continue;
      }

      SMGEdgePointsTo nextPointerEdge = smg.getPointer(nextPointer);
      int hfo = nextPointerEdge.getOffset();
      SMGTargetSpecifier nextPointerTg = nextPointerEdge.getTargetSpecifier();

      if (!(nextPointerTg == SMGTargetSpecifier.REGION
          || nextPointerTg == SMGTargetSpecifier.FIRST)) {
        continue;
      }

      SMGObject nextObject = nextPointerEdge.getObject();

      Set<SMGEdgeHasValue> nextObjectHves =
          smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(nextObject));

      if (nextObjectHves.size() < 2) {
        continue;
      }

      for (SMGEdgeHasValue hvePrev : nextObjectHves) {

        int pfo = hvePrev.getOffset();
        int prevPointer = hvePrev.getValue();

        if (!(nfo < pfo)) {
          continue;
        }

        if (!smg.isPointer(prevPointer)) {
          continue;
        }

        SMGEdgePointsTo prevPointerEdge = smg.getPointer(prevPointer);

        if (prevPointerEdge.getOffset() != hfo) {
          continue;
        }

        SMGTargetSpecifier prevPointerTg = prevPointerEdge.getTargetSpecifier();

        if (!(prevPointerTg == SMGTargetSpecifier.REGION
            || prevPointerTg == SMGTargetSpecifier.LAST)) {
          continue;
        }

        if (pObject != prevPointerEdge.getObject()) {
          continue;
        }

        SMGDoublyLinkedListCandidate candidate =
            new SMGDoublyLinkedListCandidate(pObject, hfo, pfo, nfo);
        candidates.get(pObject).put(Pair.of(nfo, pfo), candidate);
        candidateLength.put(candidate, 0);
        continueTraversal(nextPointer, candidate);
      }
    }
  }


  private void continueTraversal(int pValue, SMGDoublyLinkedListCandidate pPrevCandidate) {
    SMGEdgePointsTo pt = smg.getPointer(pValue);
    SMGObject object = pt.getObject();

    if (!smg.isHeapObject(object)) {
      return;
    }

    if (!candidates.containsKey(object)) {
      startTraversal(object);
    }

    if (inboundPointers.get(pValue) > 2) {
      return;
    }

    Map<Pair<Integer, Integer>, SMGDoublyLinkedListCandidate> objectCandidates =
        candidates.get(object);
    Integer nfo = pPrevCandidate.getNfo();
    Integer pfo = pPrevCandidate.getPfo();

    if (!objectCandidates.containsKey(Pair.of(nfo, pfo))) {
      return;
    }

    SMGDoublyLinkedListCandidate candidate = objectCandidates.get(Pair.of(nfo, pfo));

    Set<SMGEdgeHasValue> prevEdges = smg.getHVEdges(
        SMGEdgeHasValueFilter.objectFilter(candidate.getObject()).filterAtOffset(pfo));

    if (prevEdges.size() != 1) {
      return;
    }

    SMGEdgeHasValue prevEdge = Iterables.getOnlyElement(prevEdges);

    if (inboundPointers.get(prevEdge.getValue()) > 2) {
      return;
    }

    Set<SMGEdgeHasValue> nextEdges = smg.getHVEdges(
        SMGEdgeHasValueFilter.objectFilter(candidate.getObject()).filterAtOffset(nfo));

    if (nextEdges.size() != 1) {
      return;
    }

    SMGEdgeHasValue nextEdge = Iterables.getOnlyElement(nextEdges);

    continueTraversal(nextEdge.getValue(), candidate);

    candidateLength.put(pPrevCandidate, candidateLength.get(candidate) + 1);
    //Check other conditions of largest mergeable Sequence later
  }
}