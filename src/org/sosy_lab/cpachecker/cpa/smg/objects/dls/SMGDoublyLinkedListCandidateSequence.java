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
package org.sosy_lab.cpachecker.cpa.smg.objects.dls;

import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;

public class SMGDoublyLinkedListCandidateSequence implements SMGAbstractionCandidate {

  private final SMGDoublyLinkedListCandidate candidate;
  private final int length;

  public SMGDoublyLinkedListCandidateSequence(
      SMGDoublyLinkedListCandidate pCandidate,
      int pLength) {
    candidate = pCandidate;
    length = pLength;
  }

  public SMGDoublyLinkedListCandidate getCandidate() {
    return candidate;
  }

  public int getLength() {
    return length;
  }

  @Override
  public CLangSMG execute(CLangSMG pSMG) {

    return null;
  }

  @Override
  public String toString() {
    return "SMGDoublyLinkedListCandidateSequence [candidate=" + candidate + ", length=" + length
        + "]";
  }
}