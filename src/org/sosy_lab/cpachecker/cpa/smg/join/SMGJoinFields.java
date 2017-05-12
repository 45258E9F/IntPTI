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

import com.google.common.collect.Iterables;

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class SMGJoinFields {
  private final SMG newSMG1;
  private final SMG newSMG2;
  private SMGJoinStatus status = SMGJoinStatus.EQUAL;

  /**
   * SMGJoinFields is the SMG reinterpretation operation that ensures that two objects have the
   * identical fields.
   *
   * @param pSMG1 1st input SMG --- could be updated
   * @param pSMG2 2nd input SMG --- could be updated
   * @param pObj1 1st object
   * @param pObj2 2nd object
   */
  public SMGJoinFields(SMG pSMG1, SMG pSMG2, SMGObject pObj1, SMGObject pObj2) {
    if (pObj1.getSize() != pObj2.getSize()) {
      throw new IllegalArgumentException(
          "SMGJoinFields object arguments need to have identical size");
    }
    if (!(pSMG1.getObjects().contains(pObj1) && pSMG2.getObjects().contains(pObj2))) {
      throw new IllegalArgumentException(
          "SMGJoinFields object arguments need to be included in parameter SMGs");
    }

    Set<SMGEdgeHasValue> H1Prime = getCompatibleHVEdgeSet(pSMG1, pSMG2, pObj1, pObj2);
    Set<SMGEdgeHasValue> H2Prime = getCompatibleHVEdgeSet(pSMG2, pSMG1, pObj2, pObj1);

    SMG origSMG1 = new SMG(pSMG1);
    SMG origSMG2 = new SMG(pSMG2);

    pSMG1.replaceHVSet(H1Prime);
    pSMG2.replaceHVSet(H2Prime);

    status = joinFieldsRelaxStatus(origSMG1, pSMG1, status, SMGJoinStatus.RIGHT_ENTAIL, pObj1);
    status = joinFieldsRelaxStatus(origSMG2, pSMG2, status, SMGJoinStatus.LEFT_ENTAIL, pObj2);

    Set<SMGEdgeHasValue> smg2Extension = mergeNonNullHasValueEdges(pSMG1, pSMG2, pObj1, pObj2);
    Set<SMGEdgeHasValue> smg1Extension = mergeNonNullHasValueEdges(pSMG2, pSMG1, pObj2, pObj1);

    H1Prime.addAll(smg1Extension);
    H2Prime.addAll(smg2Extension);

    pSMG1.replaceHVSet(H1Prime);
    pSMG2.replaceHVSet(H2Prime);

    // here has-value edge sets on SMG1 and SMG2 are compatible, such that:
    // (1) each non-null has-value edge in SMG1 should have the matching one in SMG2
    // (2) each null has-value edge in SMG1 should have the matching null edge or pointer edge in
    // SMG2 and vice versa
    newSMG1 = pSMG1;
    newSMG2 = pSMG2;
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

  public SMG getSMG1() {
    return newSMG1;
  }

  public SMG getSMG2() {
    return newSMG2;
  }

  /**
   * Get the set of has-value edges on pObj2 which is missing and should match the corresponding
   * has-value edge on pObj1.
   */
  public static Set<SMGEdgeHasValue> mergeNonNullHasValueEdges(
      SMG pSMG1,
      SMG pSMG2,
      SMGObject pObj1,
      SMGObject pObj2) {
    Set<SMGEdgeHasValue> returnSet = new HashSet<>();

    SMGEdgeHasValueFilter filterForSMG1 = SMGEdgeHasValueFilter.objectFilter(pObj1);
    SMGEdgeHasValueFilter filterForSMG2 = SMGEdgeHasValueFilter.objectFilter(pObj2);
    filterForSMG1.filterNotHavingValue(pSMG1.getNullValue());

    for (SMGEdgeHasValue edge : pSMG1.getHVEdges(filterForSMG1)) {
      filterForSMG2.filterAtOffset(edge.getOffset());
      filterForSMG2.filterByType(edge.getType());
      if (pSMG2.getHVEdges(filterForSMG2).size() == 0) {
        returnSet.add(new SMGEdgeHasValue(edge.getType(), edge.getOffset(), pObj2,
            SMGValueFactory.getNewValue()));
      }
    }

    return Collections.unmodifiableSet(returnSet);
  }

  public static SMGJoinStatus joinFieldsRelaxStatus(
      SMG pOrigSMG, SMG pNewSMG,
      SMGJoinStatus pCurStatus, SMGJoinStatus pNewStatus, SMGObject pObject) {
    BitSet origNull = pOrigSMG.getNullBytesForObject(pObject);
    BitSet newNull = pNewSMG.getNullBytesForObject(pObject);

    for (int i = 0; i < origNull.length(); i++) {
      if (origNull.get(i) && (!newNull.get(i))) {
        return SMGJoinStatus.updateStatus(pCurStatus, pNewStatus);
      }
    }

    return pCurStatus;
  }

  /**
   * Get the set of has-value edges on pObj1 such that: (1) all non-null has-value edges are from
   * pSMG1, (2) null has-value edges from pSMG1 whose corresponding has-value edges are null or
   * pointer on pObj2.
   */
  static public Set<SMGEdgeHasValue> getCompatibleHVEdgeSet(
      SMG pSMG1,
      SMG pSMG2,
      SMGObject pObj1,
      SMGObject pObj2) {
    Set<SMGEdgeHasValue> newHVSet = SMGJoinFields.getHVSetWithoutNullValuesOnObject(pSMG1, pObj1);

    newHVSet.addAll(SMGJoinFields.getHVSetOfCommonNullValues(pSMG1, pSMG2, pObj1, pObj2));
    newHVSet.addAll(SMGJoinFields.getHVSetOfMissingNullValues(pSMG1, pSMG2, pObj1, pObj2));

    return newHVSet;
  }

  /**
   * Get the null has-value edges on pObj1 whose corresponding has-value edge has pointer value
   * in pObj2.
   */
  static public Set<SMGEdgeHasValue> getHVSetOfMissingNullValues(
      SMG pSMG1,
      SMG pSMG2,
      SMGObject pObj1,
      SMGObject pObj2) {
    Set<SMGEdgeHasValue> retset = new HashSet<>();

    SMGEdgeHasValueFilter nonNullPtrInSmg2 = SMGEdgeHasValueFilter.objectFilter(pObj2);
    nonNullPtrInSmg2.filterNotHavingValue(pSMG2.getNullValue());

    SMGEdgeHasValueFilter nonNullPtrInSmg1 = SMGEdgeHasValueFilter.objectFilter(pObj1);
    nonNullPtrInSmg1.filterNotHavingValue(pSMG1.getNullValue());

    for (SMGEdgeHasValue edge : pSMG2.getHVEdges(nonNullPtrInSmg2)) {
      if (!pSMG2.isPointer(edge.getValue())) {
        continue;
      }

      nonNullPtrInSmg1.filterAtOffset(edge.getOffset());

      if (pSMG1.getHVEdges(nonNullPtrInSmg1).size() == 0) {
        BitSet newNullBytes = pSMG1.getNullBytesForObject(pObj1);
        int min = edge.getOffset();
        int max = edge.getOffset() + edge.getSizeInBytes(pSMG1.getMachineModel());

        if (newNullBytes.get(min) && newNullBytes.nextClearBit(min) >= max) {
          retset.add(
              new SMGEdgeHasValue(edge.getType(), edge.getOffset(), pObj1, pSMG1.getNullValue()));
        }
      }
    }
    return retset;
  }

  /**
   * Get the common null has-value edges of pObj1 and pObj2 from pSMG1 and pSMG2, respectively.
   */
  static public Set<SMGEdgeHasValue> getHVSetOfCommonNullValues(
      SMG pSMG1,
      SMG pSMG2,
      SMGObject pObj1,
      SMGObject pObj2) {
    Set<SMGEdgeHasValue> retset = new HashSet<>();
    BitSet nullBytes = pSMG1.getNullBytesForObject(pObj1);

    nullBytes.and(pSMG2.getNullBytesForObject(pObj2));

    int size = 0;
    for (int i = nullBytes.nextSetBit(0); i >= 0; i = nullBytes.nextSetBit(i + 1)) {
      size++;

      if (size > 0 && ((i + 1 == nullBytes.length()) || (nullBytes.get(i + 1) == false))) {
        SMGEdgeHasValue newHV =
            new SMGEdgeHasValue(size, (i - size) + 1, pObj1, pSMG1.getNullValue());
        retset.add(newHV);
        size = 0;
      }
    }

    return Collections.unmodifiableSet(retset);
  }

  /**
   * Remove all null has-value edges in pSMG excluding null has-value edges on pObj.
   */
  static public Set<SMGEdgeHasValue> getHVSetWithoutNullValuesOnObject(SMG pSMG, SMGObject pObj) {
    Set<SMGEdgeHasValue> retset = new HashSet<>();
    retset.addAll(pSMG.getHVEdges());

    SMGEdgeHasValueFilter nullValueFilter = SMGEdgeHasValueFilter.objectFilter(pObj);
    nullValueFilter.filterHavingValue(pSMG.getNullValue());

    retset.removeAll(pSMG.getHVEdges(nullValueFilter));

    return retset;
  }

  private static void checkResultConsistencySingleSide(
      SMG pSMG1, SMGEdgeHasValueFilter nullEdges1,
      SMG pSMG2, SMGObject pObj2, BitSet nullBytesInSMG2) throws SMGInconsistentException {
    for (SMGEdgeHasValue edgeInSMG1 : pSMG1.getHVEdges(nullEdges1)) {
      int start = edgeInSMG1.getOffset();
      int byte_after_end = start + edgeInSMG1.getSizeInBytes(pSMG1.getMachineModel());
      SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(pObj2)
          .filterAtOffset(edgeInSMG1.getOffset())
          .filterByType(edgeInSMG1.getType());
      Set<SMGEdgeHasValue> hvInSMG2Set = pSMG2.getHVEdges(filter);

      SMGEdgeHasValue hvInSMG2;
      if (hvInSMG2Set.size() > 0) {
        hvInSMG2 = Iterables.getOnlyElement(hvInSMG2Set);
      } else {
        hvInSMG2 = null;
      }

      if (hvInSMG2 == null || (nullBytesInSMG2.nextClearBit(start) < byte_after_end && !pSMG2
          .isPointer(hvInSMG2.getValue()))) {
        throw new SMGInconsistentException("SMGJoinFields output assertions do not hold");
      }
    }

  }

  public static void checkResultConsistency(SMG pSMG1, SMG pSMG2, SMGObject pObj1, SMGObject pObj2)
      throws SMGInconsistentException {
    SMGEdgeHasValueFilter nullEdges1 =
        SMGEdgeHasValueFilter.objectFilter(pObj1).filterHavingValue(pSMG1.getNullValue());
    SMGEdgeHasValueFilter nullEdges2 =
        SMGEdgeHasValueFilter.objectFilter(pObj2).filterHavingValue(pSMG2.getNullValue());
    BitSet nullBytesInSMG1 = pSMG1.getNullBytesForObject(pObj1);
    BitSet nullBytesInSMG2 = pSMG2.getNullBytesForObject(pObj2);

    if (pSMG1.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObj1)).size() != pSMG2
        .getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObj2)).size()) {
      throw new SMGInconsistentException(
          "SMGJoinFields output assertion does not hold: the objects do not have identical sets of fields");
    }

    checkResultConsistencySingleSide(pSMG1, nullEdges1, pSMG2, pObj2, nullBytesInSMG2);
    checkResultConsistencySingleSide(pSMG2, nullEdges2, pSMG1, pObj1, nullBytesInSMG1);
  }
}
