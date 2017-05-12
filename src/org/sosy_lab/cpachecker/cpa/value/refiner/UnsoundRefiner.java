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
package org.sosy_lab.cpachecker.cpa.value.refiner;

import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public interface UnsoundRefiner extends Refiner {

  /**
   * Any unsound refiner, like, e.g., the {@link ValueAnalysisImpactRefiner}
   * whose refinement procedure leaves the coverage relation in an inconsistent
   * state, must ensure that a complete re-exploration of the state-space must
   * be performed before finishing the analysis.
   *
   * To this end, all states except the root state must be removed from the
   * reached set, and a valid precision must be put in place, e.g. by calling
   * the respective {@link ARGReachedSet#removeSubtree(ARGState)} method.
   */
  void forceRestart(ReachedSet reached);
}
