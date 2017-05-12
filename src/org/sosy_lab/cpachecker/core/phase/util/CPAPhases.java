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
package org.sosy_lab.cpachecker.core.phase.util;

import org.sosy_lab.cpachecker.core.phase.CPAPhase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class CPAPhases {

  private CPAPhases() {
  }

  /**
   * Extract a {@link CPAPhase} phase of specified type from a collection of phases
   *
   * @param phases The collection of phases
   * @param pType  Specific class of phase which is desired as result
   * @return The first phase encountered which is of the specified type
   */
  public static <T extends CPAPhase> T extractPhaseByType(
      Iterable<CPAPhase> phases,
      Class<T> pType) {

    for (CPAPhase phase : phases) {
      if (pType.isInstance(phase)) {
        return (pType.cast(phase));
      }
    }

    return null;
  }

  /**
   * Extract a list of {@link CPAPhase}s which have specified type of results
   *
   * @param phases The collection of phases
   * @param pType  Specific class of phase result
   * @return A collection of phases which have desired result type
   */
  public static Collection<? extends CPAPhase> extractPhasesByResultType(
      Iterable<CPAPhase> phases, Class<?> pType) {

    List<CPAPhase> targetPhases = new ArrayList<>();

    for (CPAPhase phase : phases) {
      if (pType.isInstance(phase.getResult())) {
        targetPhases.add(phase);
      }
    }

    return targetPhases;

  }

}
