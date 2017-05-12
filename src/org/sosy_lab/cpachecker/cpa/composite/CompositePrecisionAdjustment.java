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
package org.sosy_lab.cpachecker.cpa.composite;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.logging.Level;

public class CompositePrecisionAdjustment implements PrecisionAdjustment {
  protected final ImmutableList<PrecisionAdjustment> precisionAdjustments;
  protected final ImmutableList<StateProjectionFunction> stateProjectionFunctions;
  protected final ImmutableList<PrecisionProjectionFunction> precisionProjectionFunctions;

  private final LogManager logger;

  public CompositePrecisionAdjustment(
      ImmutableList<PrecisionAdjustment> precisionAdjustments, LogManager pLogger) {
    this.precisionAdjustments = precisionAdjustments;
    logger = pLogger;

    ImmutableList.Builder<StateProjectionFunction> stateProjectionFunctions =
        ImmutableList.builder();
    ImmutableList.Builder<PrecisionProjectionFunction> precisionProjectionFunctions =
        ImmutableList.builder();

    for (int i = 0; i < precisionAdjustments.size(); i++) {
      stateProjectionFunctions.add(new StateProjectionFunction(i));
      precisionProjectionFunctions.add(new PrecisionProjectionFunction(i));
    }
    this.stateProjectionFunctions = stateProjectionFunctions.build();
    this.precisionProjectionFunctions = precisionProjectionFunctions.build();
  }

  protected static class StateProjectionFunction implements Function<AbstractState, AbstractState> {

    private final int dimension;

    public StateProjectionFunction(int d) {
      dimension = d;
    }

    @Override
    public AbstractState apply(AbstractState from) {
      return ((CompositeState) from).get(dimension);
    }
  }

  protected static class PrecisionProjectionFunction
      implements Function<Precision, Precision> {
    private final int dimension;

    public PrecisionProjectionFunction(int d) {
      dimension = d;
    }

    @Override
    public Precision apply(Precision from) {
      return ((CompositePrecision) from).get(dimension);
    }
  }

  /* (non-Javadoc)
   * @see PrecisionAdjustment#prec
   */
  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pElement,
      Precision pPrecision,
      UnmodifiableReachedSet pElements,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState) throws CPAException, InterruptedException {
    return prec0(pElement, pPrecision, pElements, projection, fullState, 1);
  }

  public Optional<PrecisionAdjustmentResult> prec0(
      AbstractState pElement,
      Precision pPrecision,
      UnmodifiableReachedSet pElements,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState,
      int depth) throws CPAException, InterruptedException {

    CompositeState comp = (CompositeState) pElement;
    CompositePrecision prec = (CompositePrecision) pPrecision;
    assert (comp.getWrappedStates().size() == prec.getPrecisions().size());
    int dim = comp.getWrappedStates().size();

    ImmutableList.Builder<AbstractState> outElements = ImmutableList.builder();
    ImmutableList.Builder<Precision> outPrecisions = ImmutableList.builder();

    boolean modified = false;
    Action action = Action.CONTINUE;

    for (int i = 0; i < dim; ++i) {
      PrecisionAdjustment precisionAdjustment = precisionAdjustments.get(i);
      AbstractState oldElement = comp.get(i);
      Precision oldPrecision = prec.get(i);
      Optional<PrecisionAdjustmentResult> out = precisionAdjustment.prec(
          oldElement, oldPrecision, pElements,
          Functions.compose(stateProjectionFunctions.get(i), projection),
          fullState
      );

      if (!out.isPresent()) {
        return Optional.absent();
      }

      PrecisionAdjustmentResult inner = out.get();

      AbstractState newElement = inner.abstractState();
      Precision newPrecision = inner.precision();
      if (inner.action() == Action.BREAK) {
        action = Action.BREAK;
      }

      if ((newElement != oldElement) || (newPrecision != oldPrecision)) {
        // something has changed
        modified = true;
      }
      outElements.add(newElement);
      outPrecisions.add(newPrecision);
    }

    AbstractState outElement = modified ? new CompositeState(outElements.build()) : pElement;
    Precision outPrecision = modified ? new CompositePrecision(outPrecisions.build()) : pPrecision;

    PrecisionAdjustmentResult out =
        PrecisionAdjustmentResult.create(outElement, outPrecision, action);

    if (!modified) {

      logger.log(Level.FINER, "Precision adjustment iteration has converged.");
      return Optional.of(out);
    } else {

      // Recursion is acceptable here as we have very small chains.
      logger.log(Level.FINER, "Starting new fixpoint iteration of precision adjustment");
      return prec0(outElement, outPrecision, pElements, projection, fullState, depth + 1);
    }
  }
}
