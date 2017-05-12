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
package org.sosy_lab.cpachecker.core.interfaces.pcc;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;


public interface PartialReachedConstructionAlgorithm {

  /**
   * Computes a subset of the reached set <code>pReached</code> which should be sufficient to be
   * used as certificate.
   *
   * @param pReached - analysis result, overapproximation of reachable states
   * @return a subset of <code>pReached</code>
   * @throws InvalidConfigurationException if abstract state format does not match expectations for
   *                                       construction
   */
  public AbstractState[] computePartialReachedSet(UnmodifiableReachedSet pReached)
      throws InvalidConfigurationException;

}
