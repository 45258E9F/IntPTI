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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.dls.SMGDoublyLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGGenericAbstractionCandidate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


final class SMGJoinSubSMGs {
  static private boolean performChecks = false;

  static public void performChecks(boolean pValue) {
    performChecks = pValue;
  }

  private SMGJoinStatus status;
  private boolean defined = false;

  private SMG inputSMG1;
  private SMG inputSMG2;
  private SMG destSMG;

  private SMGNodeMapping mapping1 = null;
  private SMGNodeMapping mapping2 = null;
  private final List<SMGGenericAbstractionCandidate> subSmgAbstractionCandidates;

  /**
   * Core function of SMG Join.
   *
   * @param initialStatus                     initial join --- could be updated
   * @param pSMG1                             1st SMG to be merged
   * @param pSMG2                             2nd SMG to be merged
   * @param pDestSMG                          merged SMG --- could be updated
   * @param pMapping1                         1st SMG node mapping -- could be updated
   * @param pMapping2                         2nd SMG node mapping -- could be updated
   * @param pObj1                             SMG object in 1st SMG
   * @param pObj2                             SMG object in 2nd SMG
   * @param pNewObject                        merged SMG object
   * @param pLDiff                            ??
   * @param pIncreaseLevelAndRelabelTargetSpc ??
   * @param identicalInputSmg                 ??
   */
  public SMGJoinSubSMGs(
      SMGJoinStatus initialStatus,
      SMG pSMG1, SMG pSMG2, SMG pDestSMG,
      SMGNodeMapping pMapping1, SMGNodeMapping pMapping2,
      SMGObject pObj1, SMGObject pObj2, SMGObject pNewObject,
      int pLDiff, boolean pIncreaseLevelAndRelabelTargetSpc, boolean identicalInputSmg)
      throws SMGInconsistentException {

    // SMGJoinFields is the join reinterpretation operator that ensures that two SMG objects have
    // the identical fields
    SMGJoinFields joinFields = new SMGJoinFields(pSMG1, pSMG2, pObj1, pObj2);
    subSmgAbstractionCandidates = ImmutableList.of();
    inputSMG1 = joinFields.getSMG1();
    inputSMG2 = joinFields.getSMG2();

    if (SMGJoinSubSMGs.performChecks) {
      SMGJoinFields.checkResultConsistency(inputSMG1, inputSMG2, pObj1, pObj2);
    }

    destSMG = pDestSMG;
    status = SMGJoinStatus.updateStatus(initialStatus, joinFields.getStatus());
    mapping1 = pMapping1;
    mapping2 = pMapping2;

    /*
     * After joinFields, the objects have identical set of fields. Therefore, to iterate
     * over them, it is sufficient to loop over HV set in the first SMG, and just
     * obtain the (always just single one) corresponding edge from the second
     * SMG.
     */

    SMGEdgeHasValueFilter filterOnSMG1 = SMGEdgeHasValueFilter.objectFilter(pObj1);
    SMGEdgeHasValueFilter filterOnSMG2 = SMGEdgeHasValueFilter.objectFilter(pObj2);

    Set<SMGEdgeHasValue> edgesOnObject1 = Sets.newHashSet(inputSMG1.getHVEdges(filterOnSMG1));

    Map<Integer, List<SMGGenericAbstractionCandidate>> valueAbstractionCandidates = new HashMap<>();
    boolean allValuesDefined = true;

    int lDiff = pLDiff;
    boolean object1IsDLS = pObj1 instanceof SMGDoublyLinkedList;
    boolean object2IsDLS = pObj2 instanceof SMGDoublyLinkedList;

    int nfo1 = -1;
    int pfo1 = -1;
    int nfo2 = -1;
    int pfo2 = -1;

    if (object1IsDLS) {
      SMGDoublyLinkedList dls = (SMGDoublyLinkedList) pObj1;
      nfo1 = dls.getNfo();
      pfo1 = dls.getPfo();
    }

    if (object2IsDLS) {
      SMGDoublyLinkedList dls = (SMGDoublyLinkedList) pObj2;
      nfo2 = dls.getNfo();
      pfo2 = dls.getPfo();
    }

    for (SMGEdgeHasValue hvIn1 : edgesOnObject1) {
      filterOnSMG2.filterAtOffset(hvIn1.getOffset());
      filterOnSMG2.filterByType(hvIn1.getType());
      // since two SMGs are compatible, hvIn2 must be non-null
      SMGEdgeHasValue hvIn2 = Iterables.getOnlyElement(inputSMG2.getHVEdges(filterOnSMG2));

      if (object1IsDLS && hvIn1.getOffset() != nfo1 && hvIn1.getOffset() != pfo1) {
        lDiff = lDiff + 1;
      }

      if (object2IsDLS && hvIn1.getOffset() != nfo2 && hvIn1.getOffset() != pfo2) {
        lDiff = lDiff - 1;
      }

      SMGJoinValues joinValues = new SMGJoinValues(status, inputSMG1, inputSMG2, destSMG,
          mapping1, mapping2, hvIn1.getValue(), hvIn2.getValue(), pLDiff,
          pIncreaseLevelAndRelabelTargetSpc, identicalInputSmg, pObj1.getLevel(), pObj2.getLevel());

      /* If the join of the values is not defined and can't be
       * recovered through abstraction, the join fails.*/
      if (!joinValues.isDefined() && !joinValues.isRecoverable()) {
        //subSmgAbstractionCandidates = ImmutableList.of();
        return;
      }

      status = joinValues.getStatus();
      inputSMG1 = joinValues.getInputSMG1();
      inputSMG2 = joinValues.getInputSMG2();
      destSMG = joinValues.getDestinationSMG();
      mapping1 = joinValues.getMapping1();
      mapping2 = joinValues.getMapping2();

      if (joinValues.isDefined()) {

        SMGEdgeHasValue newHV;

        if (hvIn1.getObject().equals(pNewObject)
            && joinValues.getValue().equals(hvIn1.getValue())) {
          newHV = hvIn1;
        } else {
          newHV = new SMGEdgeHasValue(hvIn1.getType(), hvIn1.getOffset(), pNewObject,
              joinValues.getValue());
        }

        destSMG.addHasValueEdge(newHV);

        if (joinValues.subSmgHasAbstractionsCandidates()) {
          valueAbstractionCandidates
              .put(joinValues.getValue(), joinValues.getAbstractionCandidates());
        }
      } else {
        // that means, the joined value is undefined but recoverable by abstraction
        allValuesDefined = false;
      }
    }

    /* If the join is defined without abstraction candidates in
       sub smgs, we don't need to perform abstraction.*/
    if (allValuesDefined && valueAbstractionCandidates.isEmpty()) {
      defined = true;
      //subSmgAbstractionCandidates = ImmutableList.of();
      return;
    }

    //SMGJoinAbstractionManager abstractionManager = new SMGJoinAbstractionManager(pObj1, pObj2, inputSMG1, inputSMG2, pNewObject, destSMG);
    //subSmgAbstractionCandidates = abstractionManager.calculateCandidates(valueAbstractionCandidates);

    /*If abstraction candidates can be found for this sub Smg, then the join for this sub smg
     *  is defined under the assumption, that the abstraction of one abstraction candidate is executed.*/
    //if (!subSmgAbstractionCandidates.isEmpty()) {
    //defined = true;
    //return;
    //}

    /* If no abstraction can be found for this sub Smg, then the join is only defined,
     * if all values are defined. For values that are defined under the assumption,
     * that a abstraction candidate is execued for the destination smg, execute the abstraction
     * so that the join of this sub SMG is complete.*/
    if (!allValuesDefined) {
      defined = false;
      return;
    }

    for (List<SMGGenericAbstractionCandidate> abstractionCandidates : valueAbstractionCandidates
        .values()) {
      Collections.sort(abstractionCandidates);
      abstractionCandidates.get(0).execute(destSMG);
    }

    defined = true;
  }

  public boolean isDefined() {
    return defined;
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

  public SMG getSMG1() {
    return inputSMG1;
  }

  public SMG getSMG2() {
    return inputSMG2;
  }

  public SMG getDestSMG() {
    return destSMG;
  }

  public SMGNodeMapping getMapping1() {
    return mapping1;
  }

  public SMGNodeMapping getMapping2() {
    return mapping2;
  }

  public List<SMGGenericAbstractionCandidate> getSubSmgAbstractionCandidates() {
    return subSmgAbstractionCandidates;
  }
}