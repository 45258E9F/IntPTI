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
package org.sosy_lab.cpachecker.cpa.bam;

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
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.util.logging.Level;

public class BAMPrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment wrappedPrecisionAdjustment;
  private final BAMTransferRelation trans;
  private final BAMDataManager data;
  private final LogManager logger;

  public BAMPrecisionAdjustment(
      PrecisionAdjustment pWrappedPrecisionAdjustment,
      BAMDataManager pData, BAMTransferRelation pTransfer, LogManager pLogger) {
    this.wrappedPrecisionAdjustment = pWrappedPrecisionAdjustment;
    this.data = pData;
    this.trans = pTransfer;
    this.logger = pLogger;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pElement, Precision pPrecision,
      UnmodifiableReachedSet pElements, Function<AbstractState, AbstractState> projection,
      AbstractState fullState) throws CPAException, InterruptedException {
    if (trans.breakAnalysis) {
      return Optional.of(
          PrecisionAdjustmentResult.create(pElement, pPrecision, Action.BREAK));
    }

    // precision might be outdated, if comes from a block-start and the inner part was refined.
    // so lets use the (expanded) inner precision.
    final Precision validPrecision;
    if (data.expandedStateToExpandedPrecision.containsKey(pElement)) {
      assert AbstractStates.isTargetState(pElement)
          || trans.getBlockPartitioning().isReturnNode(AbstractStates.extractLocation(pElement));
      validPrecision = data.expandedStateToExpandedPrecision.get(pElement);
    } else {
      validPrecision = pPrecision;
    }

    Optional<PrecisionAdjustmentResult> result = wrappedPrecisionAdjustment.prec(
        pElement,
        validPrecision,
        pElements,
        projection,
        fullState);
    if (!result.isPresent()) {
      return result;
    }

    PrecisionAdjustmentResult updatedResult = result.get().withAbstractState(
        trans.attachAdditionalInfoToCallNode(result.get().abstractState()));

    if (pElement != updatedResult.abstractState()) {
      logger.log(Level.ALL, "before PREC:", pElement);
      logger.log(Level.ALL, "after PREC:", updatedResult.abstractState());
      data.replaceStateInCaches(pElement, updatedResult.abstractState(), false);
    }

    return Optional.of(updatedResult);
  }
}
