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
package org.sosy_lab.cpachecker.util.refinement;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Set;

public interface Interpolant<S extends AbstractState> {

  /**
   * Reconstructs a valid state from the interpolant.
   */
  S reconstructState();

  /**
   * Returns the size of the interpolant.
   */
  int getSize();

  /**
   * Returns the memory locations this interpolant uses.
   */
  Set<MemoryLocation> getMemoryLocations();

  boolean isTrue();

  boolean isFalse();

  boolean isTrivial();

  <T extends Interpolant<S>> T join(T otherInterpolant);
}
