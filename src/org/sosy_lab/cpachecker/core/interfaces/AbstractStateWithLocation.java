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
package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * This interface represents abstract states that
 * somehow store information about which CFA location the abstract state
 * belongs to.
 * The interface is intended to provide this knowledge about the location
 * to other components, such as other CPAs or algorithms.
 *
 * The method {@link AbstractStates#extractLocation(AbstractState)}
 * provides a convenient way to access this information.
 */
public interface AbstractStateWithLocation extends AbstractStateWithLocations {

  /**
   * Get the {@link CFANode} that represents the location of this state.
   *
   * @return A node of the CFA.
   */
  CFANode getLocationNode();
}
