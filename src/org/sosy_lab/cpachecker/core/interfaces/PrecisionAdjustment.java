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


import com.google.common.base.Function;
import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Interface for the precision adjustment operator.
 */
public interface PrecisionAdjustment {

  /**
   * This method may adjust the current abstractState and precision using information
   * from the current set of reached states.
   *
   * If this method doesn't change anything, it is strongly recommended to return
   * the identical objects for abstractState and precision. This makes it easier for
   * wrapper CPAs.
   *
   * @param state           The current abstract state for this CPA.
   * @param precision       The current precision for this CPA.
   * @param states          The current reached set with ALL abstract states.
   * @param stateProjection Projection function from any state within reached set to a state
   *                        belonging to this CPA.
   * @param fullState       The current abstract state, but for all CPAs (This can be used to access
   *                        information stored in abstract states of other CPAs such as the current
   *                        CFA location. Use methods from {@link AbstractStates} to access the
   *                        individual states.).
   * @return The new abstract state, new precision and the action flag encapsulated in a {@link
   * PrecisionAdjustmentResult} instance OR Optional.absent() if the newly produced abstract states
   * corresponds to BOTTOM.
   */
  Optional<PrecisionAdjustmentResult> prec(
      AbstractState state,
      Precision precision,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState fullState
  ) throws CPAException, InterruptedException;
}
