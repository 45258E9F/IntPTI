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
package org.sosy_lab.cpachecker.core.phase.result;


public enum CPAPhaseStatus {
  SUCCESS,
  FAIL;

  /**
   * Merge two results, in the future we may enrich the types of results
   *
   * @param r1 The first result
   * @param r2 The second result
   * @return The merged result
   */
  public static CPAPhaseStatus mergeResult(CPAPhaseStatus r1, CPAPhaseStatus r2) {
    if (r1 == FAIL || r2 == FAIL) {
      return FAIL;
    } else if (r1 == SUCCESS && r2 == SUCCESS) {
      return SUCCESS;
    } else {
      throw new RuntimeException(String.format("Unhandled CPAPhaseStatus: (%s, %s)", r1, r2));
    }
  }

  public static CPAPhaseStatus mergeResult3(
      CPAPhaseStatus r1,
      CPAPhaseStatus r2,
      CPAPhaseStatus r3) {
    return mergeResult(mergeResult(r1, r2), r3);
  }

}
