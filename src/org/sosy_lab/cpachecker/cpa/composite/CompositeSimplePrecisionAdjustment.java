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
package org.sosy_lab.cpachecker.cpa.composite;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.core.defaults.BreakOnTargetsPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.SimplePrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Alternative to {@link CompositeSimplePrecisionAdjustment} which is faster
 * but can only be used if all child prec operators are either
 * {@link BreakOnTargetsPrecisionAdjustment} or {@link StaticPrecisionAdjustment}.
 *
 * It works by storing a list of indices and then just checks the elements at
 * these indexes if they are targets.
 * It does never call other precision adjustment operators.
 */
class CompositeSimplePrecisionAdjustment extends SimplePrecisionAdjustment {

  private final ImmutableList<SimplePrecisionAdjustment> precisionAdjustments;

  public CompositeSimplePrecisionAdjustment(ImmutableList<SimplePrecisionAdjustment> precisionAdjustments) {
    this.precisionAdjustments = precisionAdjustments;
  }

  @Override
  public Action prec(AbstractState pElement, Precision pPrecision) throws CPAException {
    CompositeState comp = (CompositeState) pElement;
    CompositePrecision prec = (CompositePrecision) pPrecision;
    assert (comp.getWrappedStates().size() == prec.getPrecisions().size());
    int dim = comp.getNumberOfStates();

    for (int i = 0; i < dim; ++i) {
      SimplePrecisionAdjustment precisionAdjustment = precisionAdjustments.get(i);
      AbstractState oldElement = comp.get(i);
      Precision oldPrecision = prec.get(i);
      Action action = precisionAdjustment.prec(oldElement, oldPrecision);

      if (action == Action.BREAK) {
        return Action.BREAK;
      }
    }

    return Action.CONTINUE;
  }
}