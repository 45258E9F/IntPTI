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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.base.Objects;

import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Represents the result to a call to {@link PrecisionAdjustment#prec(AbstractState, Precision,
 * UnmodifiableReachedSet, Function, AbstractState)}. Contains the (possibly changed) abstract
 * abstractState and precision, and an {@link PrecisionAdjustmentResult.Action} instance (all are
 * not null).
 */
@Immutable
public class PrecisionAdjustmentResult {

  private final AbstractState abstractState;
  private final Precision precision;
  private final Action action;

  /**
   * The precision adjustment operator can tell the CPAAlgorithm whether
   * to continue with the analysis or whether to break immediately.
   */
  public enum Action {
    CONTINUE,
    BREAK,;
  }

  private PrecisionAdjustmentResult(
      AbstractState pState, Precision pPrecision,
      Action pAction) {
    abstractState = checkNotNull(pState);
    precision = checkNotNull(pPrecision);
    action = checkNotNull(pAction);
  }


  public static PrecisionAdjustmentResult create(
      AbstractState pState,
      Precision pPrecision, Action pAction) {
    return new PrecisionAdjustmentResult(pState, pPrecision, pAction);
  }

  public AbstractState abstractState() {
    return abstractState;
  }

  public Precision precision() {
    return precision;
  }

  public Action action() {
    return action;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(abstractState, action, precision);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PrecisionAdjustmentResult)) {
      return false;
    }
    PrecisionAdjustmentResult other = (PrecisionAdjustmentResult) obj;
    return (abstractState.equals(other.abstractState))
        && (precision.equals(other.precision))
        && (action.equals(other.action))
        ;
  }

  public PrecisionAdjustmentResult withAbstractState(AbstractState newAbstractState) {
    return new PrecisionAdjustmentResult(newAbstractState, precision, action);
  }

  public PrecisionAdjustmentResult withPrecision(Precision newPrecision) {
    return new PrecisionAdjustmentResult(abstractState, newPrecision, action);
  }

  public PrecisionAdjustmentResult withAction(Action newAction) {
    return new PrecisionAdjustmentResult(abstractState, precision, newAction);
  }
}
