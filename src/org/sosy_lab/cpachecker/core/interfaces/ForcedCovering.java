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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Interface for implementations of forced coverings
 * (strengthening a new abstract state such that it is covered by another state
 * from the reached set).
 *
 * Implementations need to have a public constructor which takes a
 * {@link Configuration}, a {@link LogManager}, and a
 * {@link ConfigurableProgramAnalysis}, and throws at most an
 * {@link InvalidConfigurationException}.
 */
public interface ForcedCovering {

  /**
   * Try to cover the abstract state by strengthening it (and possibly its parents).
   *
   * This method should not change the reached set, except by re-adding some
   * state to the waitlist. It is necessary to re-add states to the waitlist,
   * which are covered by strengthened state, and this method is responsible
   * for this!
   *
   * The methods returns a boolean indicating success in covering the state
   * or not. This means, if this method returns true, the stop operator called
   * with the same arguments after this method returned also needs to return true.
   *
   * @param state     The state which hopefully is covered afterwards.
   * @param precision The precision for the state.
   * @param reached   The current reached set.
   * @return Whether forced covering was successful.
   */
  boolean tryForcedCovering(AbstractState state, Precision precision, ReachedSet reached)
      throws CPAException, InterruptedException;
}