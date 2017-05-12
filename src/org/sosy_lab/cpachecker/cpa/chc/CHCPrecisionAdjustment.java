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
package org.sosy_lab.cpachecker.cpa.chc;

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


public class CHCPrecisionAdjustment implements PrecisionAdjustment {

  private final LogManager logger;

  public CHCPrecisionAdjustment(LogManager logM) {
    logger = logM;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState successor, Precision precision,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState) throws CPAException {

    CHCState candidateState = (CHCState) successor;

    CHCState ancestor = findVariantAncestor(candidateState);

    if (ancestor != null) {
      AbstractState newState = generalize(candidateState, ancestor);
      return Optional.of(PrecisionAdjustmentResult
          .create(newState, precision, Action.CONTINUE));
    } else {
      return Optional.of(PrecisionAdjustmentResult.create(successor, precision, Action.CONTINUE));
    }

  }

  private CHCState findVariantAncestor(CHCState candidateState) {
    CHCState variantAncestor = candidateState.getAncestor();
    while (variantAncestor != null) {
      if (variantAncestor.getNodeId() == candidateState.getNodeId()) {
        logger.log(Level.FINEST, "\n * variant found: " + variantAncestor.toString());
        return variantAncestor;
      }
      variantAncestor = variantAncestor.getAncestor();
    }

    return null;

  }

  /**
   * Compute a generalization of reachedState w.r.t. one of its ancestors
   */
  private AbstractState generalize(CHCState reachedState, CHCState ancestor) {
    CHCState gState = new CHCState();

    gState.setConstraint(
        ConstraintManager.generalize(ancestor.getConstraint(), reachedState.getConstraint()));
    return gState;
  }

}