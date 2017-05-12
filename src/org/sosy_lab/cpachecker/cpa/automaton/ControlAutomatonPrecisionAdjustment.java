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
package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import javax.annotation.Nullable;

@Options(prefix = "cpa.automaton.prec")
public class ControlAutomatonPrecisionAdjustment implements PrecisionAdjustment {

  private final
  @Nullable
  PrecisionAdjustment wrappedPrec;
  private final AutomatonState topState;

  @Option(secure = true, name = "topOnFinalSelfLoopingState",
      description = "An implicit precision: consider states with a self-loop and no other outgoing edges as TOP.")
  private boolean topOnFinalSelfLoopingState = false;

  public ControlAutomatonPrecisionAdjustment(
      Configuration pConfig,
      AutomatonState pTopState,
      PrecisionAdjustment pWrappedPrecisionAdjustment)
      throws InvalidConfigurationException {

    pConfig.inject(this);

    this.topState = pTopState;
    this.wrappedPrec = pWrappedPrecisionAdjustment;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pStateProjection,
      AbstractState pFullState)
      throws CPAException, InterruptedException {

    Optional<PrecisionAdjustmentResult> wrappedPrecResult = wrappedPrec.prec(pState,
        pPrecision, pStates, pStateProjection, pFullState);

    if (!wrappedPrecResult.isPresent()) {
      return wrappedPrecResult;
    }

    AutomatonInternalState internalState = ((AutomatonState) pState).getInternalState();

    // Handle the BREAK state
    if (internalState.getName().equals(AutomatonInternalState.BREAK.getName())) {
      return Optional.of(wrappedPrecResult.get().withAction(Action.BREAK));
    }

    // Handle SINK state
    if (topOnFinalSelfLoopingState
        && internalState.isFinalSelfLoopingState()) {

      AbstractState adjustedSate = topState;
      Precision adjustedPrecision = pPrecision;
      return Optional.of(PrecisionAdjustmentResult.create(
          adjustedSate,
          adjustedPrecision, Action.CONTINUE));
    }

    return wrappedPrecResult;
  }

}
