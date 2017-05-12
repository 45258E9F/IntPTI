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
package org.sosy_lab.cpachecker.core.defaults;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Base implementation for precision adjustment implementations which fulfill
 * these three requirements:
 * - prec does not change the state
 * - prec does not change the precision
 * - prec does not need access to the reached set
 *
 * By inheriting from this class, implementations give callers the opportunity
 * to directly call {@link #prec(AbstractState, Precision)}, which is faster.
 */
public abstract class SimplePrecisionAdjustment implements PrecisionAdjustment {

  @Override
  public final Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState, Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState) throws CPAException {

    Action action = prec(pState, pPrecision);

    return Optional.of(PrecisionAdjustmentResult.create(pState, pPrecision, action));
  }

  public abstract Action prec(AbstractState pState, Precision pPrecision) throws CPAException;
}
