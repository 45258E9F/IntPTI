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
package org.sosy_lab.cpachecker.cpa.constraints.refiner;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.ConstraintsPrecision;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.io.PrintStream;
import java.util.Collection;

import javax.annotation.Nullable;

/**
 * {@link PrecisionAdjustment} for
 * {@link org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA ConstraintsCPA}.
 */
public class ConstraintsPrecisionAdjustment implements PrecisionAdjustment, StatisticsProvider {

  // Statistics
  private int maxFullConstraintNumber = 0;
  private int maxRealConstraintNumber = 0;
  private int overallFullConstraintNumber = 0;
  private int overallRealConstraintNumber = 0;
  private final Timer adjustmentTime = new Timer();

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      final AbstractState pStateToAdjust,
      final Precision pPrecision,
      final UnmodifiableReachedSet pReachedStates,
      final Function<AbstractState, AbstractState> pStateProjection,
      final AbstractState pFullState
  ) {

    return prec((ConstraintsState) pStateToAdjust,
        (ConstraintsPrecision) pPrecision,
        pFullState);
  }

  private Optional<PrecisionAdjustmentResult> prec(
      final ConstraintsState pStateToAdjust,
      final ConstraintsPrecision pPrecision,
      final AbstractState pFullState
  ) {

    int fullConstraintNumber = 0;
    int realConstraintNumber = 0;


    adjustmentTime.start();
    ConstraintsState result = pStateToAdjust.copyOf();

    try {
      for (Constraint c : pStateToAdjust) {
        fullConstraintNumber++;
        overallFullConstraintNumber++;
        CFANode currentLocation = AbstractStates.extractLocation(pFullState);

        if (!pPrecision.isTracked(c, currentLocation)) {
          result.remove(c);

        } else {
          realConstraintNumber++;
          overallRealConstraintNumber++;
        }
      }
    } finally {
      adjustmentTime.stop();
    }

    if (fullConstraintNumber > maxFullConstraintNumber) {
      maxFullConstraintNumber = fullConstraintNumber;
    }

    if (realConstraintNumber > maxRealConstraintNumber) {
      maxRealConstraintNumber = realConstraintNumber;
    }

    result = result.equals(pStateToAdjust) ? pStateToAdjust : result;

    return Optional.of(PrecisionAdjustmentResult.create(result, pPrecision, Action.CONTINUE));
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(new Statistics() {

      @Override
      public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
        out.println("Most constraints after refinement in state: " + maxRealConstraintNumber);
        out.println("Most constraints before refinement in state: " + maxFullConstraintNumber);
        out.println("Constraints after refinement in state: " + overallRealConstraintNumber);
        out.println("Constraints before refinement in state: " + overallFullConstraintNumber);
        out.println("Average time for constraints adjustment: " + adjustmentTime.getAvgTime());
        out.println("Complete time for constraints adjustment: " + adjustmentTime.getSumTime());
      }

      @Nullable
      @Override
      public String getName() {
        return ConstraintsPrecisionAdjustment.this.getClass().getSimpleName();
      }
    });
  }
}
