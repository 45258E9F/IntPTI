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
package org.sosy_lab.cpachecker.cpa.arg;

import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Alternative to {@link Refiner} for refiners that are based on using an ARG. The refiner is
 * supplied with the error path through the ARG on refinement.
 *
 * Use {@link AbstractARGBasedRefiner#forARGBasedRefiner(ARGBasedRefiner,
 * org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis)} to create a {@link Refiner}
 * instance from an instance of this interface.
 */
public interface ARGBasedRefiner {

  /**
   * Perform refinement.
   *
   * @param pReached the reached set
   * @param pPath    the potential error path
   * @return Information about the counterexample.
   */
  CounterexampleInfo performRefinementForPath(ARGReachedSet pReached, ARGPath pPath)
      throws CPAException, InterruptedException;
}
