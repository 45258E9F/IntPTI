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
import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Base implementation for precision adjustment implementations wrap other
 * precision adjustment operators.
 */
public abstract class WrappingPrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment wrappedPrecOp;

  public WrappingPrecisionAdjustment(PrecisionAdjustment pWrappedPrecOp) {
    this.wrappedPrecOp = Preconditions.checkNotNull(pWrappedPrecOp);
  }

  protected abstract Optional<PrecisionAdjustmentResult> wrappingPrec(
      AbstractState pState, Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pProjection,
      AbstractState pFullState) throws CPAException;

  @Override
  public final Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState, Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pProjection,
      AbstractState pFullState) throws CPAException, InterruptedException {

    Optional<PrecisionAdjustmentResult> result =
        wrappedPrecOp.prec(pState, pPrecision, pStates, pProjection, pFullState);

    if (result.isPresent()) {
      if (result.get().action() == Action.BREAK) {
        return result;
      } else {
        return wrappingPrec(result.get().abstractState(), pPrecision, pStates, pProjection,
            pFullState);
      }
    } else {
      return wrappingPrec(pState, pPrecision, pStates, pProjection, pFullState);
    }
  }

  public abstract Action prec(AbstractState pState, Precision pPrecision) throws CPAException;
}
