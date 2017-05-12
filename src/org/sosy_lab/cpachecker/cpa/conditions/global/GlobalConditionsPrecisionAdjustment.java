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
package org.sosy_lab.cpachecker.cpa.conditions.global;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.logging.Level;


class GlobalConditionsPrecisionAdjustment implements PrecisionAdjustment {

  private final LogManager logger;

  private final GlobalConditionsThresholds thresholds;

  private final GlobalConditionsSimplePrecisionAdjustment delegate;

  GlobalConditionsPrecisionAdjustment(
      LogManager pLogger, GlobalConditionsThresholds pThresholds,
      GlobalConditionsSimplePrecisionAdjustment pDelegate) {
    logger = pLogger;
    thresholds = pThresholds;
    delegate = pDelegate;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pElement, Precision pPrecision,
      UnmodifiableReachedSet pElements,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState) throws CPAException {

    if (checkReachedSetSize(pElements)) {
      logger.log(Level.WARNING, "Reached set size threshold reached, terminating.");
      return Optional.of(PrecisionAdjustmentResult.create(pElement, pPrecision, Action.BREAK));
    }

    return Optional.of(PrecisionAdjustmentResult
        .create(pElement, pPrecision, delegate.prec(pElement, pPrecision)));
  }


  private boolean checkReachedSetSize(UnmodifiableReachedSet elements) {

    long threshold = thresholds.getReachedSetSizeThreshold();
    if (threshold >= 0) {
      return (elements.size() > threshold);
    }

    return false;
  }
}
