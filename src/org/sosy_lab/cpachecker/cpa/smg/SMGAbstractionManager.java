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
package org.sosy_lab.cpachecker.cpa.smg;

import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.sll.SMGSingleLinkedListFinder;

import java.util.ArrayList;
import java.util.List;

public class SMGAbstractionManager {
  private CLangSMG smg;
  private List<SMGAbstractionCandidate> abstractionCandidates = new ArrayList<>();

  public SMGAbstractionManager(CLangSMG pSMG) {
    smg = new CLangSMG(pSMG);
  }

  private boolean hasCandidates() {
    SMGSingleLinkedListFinder sllCandidateFinder = new SMGSingleLinkedListFinder();
    abstractionCandidates.addAll(sllCandidateFinder.traverse(smg));

    return (!abstractionCandidates.isEmpty());
  }

  private SMGAbstractionCandidate getBestCandidate() {
    return abstractionCandidates.get(0);
  }

  public CLangSMG execute() {
    while (hasCandidates()) {
      SMGAbstractionCandidate best = getBestCandidate();
      smg = best.execute(smg);
      invalidateCandidates();
    }
    return smg;
  }

  private void invalidateCandidates() {
    abstractionCandidates.clear();
  }
}
