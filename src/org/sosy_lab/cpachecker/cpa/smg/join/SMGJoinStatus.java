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


public enum SMGJoinStatus {
  EQUAL,
  LEFT_ENTAIL,
  RIGHT_ENTAIL,
  INCOMPARABLE,
  INCOMPLETE;

  public static SMGJoinStatus updateStatus(SMGJoinStatus pStatus1, SMGJoinStatus pStatus2) {
    if (pStatus1 == SMGJoinStatus.EQUAL) {
      return pStatus2;
    } else if (pStatus2 == SMGJoinStatus.EQUAL) {
      return pStatus1;
    } else if (pStatus1 == SMGJoinStatus.INCOMPARABLE ||
        pStatus2 == SMGJoinStatus.INCOMPARABLE ||
        pStatus1 != pStatus2) {
      return SMGJoinStatus.INCOMPARABLE;
    }
    return pStatus1;
  }
}
