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
package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public interface Refiner {

  /**
   * Perform refinement, if possible.
   *
   * @param pReached The reached set.
   * @return Whether the refinement was successful.
   * @throws CPAException If an error occured during refinement.
   */
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException;

}
