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

import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;


public class SMGDoublyLinkedListCandidate {

  private final SMGObject startObject;
  private final int hfo;
  private final int pfo;
  private final int nfo;

  public SMGDoublyLinkedListCandidate(SMGObject pObject, int pHfo, int pPfo, int pNfo) {
    startObject = pObject;
    hfo = pHfo;
    pfo = pPfo;
    nfo = pNfo;
  }

  @Override
  public String toString() {
    return "SMGDoublyLinkedListCandidate [startObject=" + startObject + ", hfo=" + hfo + ", pfo="
        + pfo + ", nfo=" + nfo + "]";
  }

  public boolean isConsistentWith(SMGDoublyLinkedListCandidate other) {
    return hfo == other.hfo && nfo == other.nfo && pfo == other.pfo;
  }

  public SMGObject getObject() {
    return startObject;
  }

  public int getHfo() {
    return hfo;
  }

  public int getPfo() {
    return pfo;
  }

  public int getNfo() {
    return nfo;
  }
}