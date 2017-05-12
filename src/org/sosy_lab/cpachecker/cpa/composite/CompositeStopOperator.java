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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCoveringStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CompositeStopOperator implements StopOperator, ForcedCoveringStopOperator {

  protected final ImmutableList<StopOperator> stopOperators;

  public CompositeStopOperator(ImmutableList<StopOperator> stopOperators) {
    this.stopOperators = stopOperators;
  }

  @Override
  public boolean stop(AbstractState element, Collection<AbstractState> reached, Precision precision)
      throws CPAException, InterruptedException {
    CompositeState compositeState = (CompositeState) element;
    CompositePrecision compositePrecision = (CompositePrecision) precision;

    for (AbstractState e : reached) {
      if (stop(compositeState, (CompositeState) e, compositePrecision)) {
        return true;
      }
    }
    return false;
  }

  private boolean stop(
      CompositeState compositeState,
      CompositeState compositeReachedState,
      CompositePrecision compositePrecision) throws CPAException, InterruptedException {
    List<AbstractState> compositeElements = compositeState.getWrappedStates();
    checkArgument(compositeElements.size() == stopOperators.size(),
        "State with wrong number of component states given");
    List<AbstractState> compositeReachedStates = compositeReachedState.getWrappedStates();

    List<Precision> compositePrecisions = compositePrecision.getPrecisions();

    for (int idx = 0; idx < compositeElements.size(); idx++) {
      StopOperator stopOp = stopOperators.get(idx);

      AbstractState absElem1 = compositeElements.get(idx);
      AbstractState absElem2 = compositeReachedStates.get(idx);
      Precision prec = compositePrecisions.get(idx);

      if (!stopOp.stop(absElem1, Collections.singleton(absElem2), prec)) {
        return false;
      }
    }
    return true;
  }

  boolean isCoveredBy(
      AbstractState pElement,
      AbstractState pOtherElement,
      List<ConfigurableProgramAnalysis> cpas) throws CPAException, InterruptedException {
    CompositeState compositeState = (CompositeState) pElement;
    CompositeState compositeOtherElement = (CompositeState) pOtherElement;

    List<AbstractState> componentElements = compositeState.getWrappedStates();
    List<AbstractState> componentOtherElements = compositeOtherElement.getWrappedStates();

    if (componentElements.size() != cpas.size()) {
      return false;
    }

    for (int idx = 0; idx < componentElements.size(); idx++) {
      ProofChecker componentProofChecker = (ProofChecker) cpas.get(idx);

      AbstractState absElem1 = componentElements.get(idx);
      AbstractState absElem2 = componentOtherElements.get(idx);

      if (!componentProofChecker.isCoveredBy(absElem1, absElem2)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean isForcedCoveringPossible(
      AbstractState pElement,
      AbstractState pReachedState,
      Precision pPrecision) throws CPAException, InterruptedException {

    CompositeState compositeState = (CompositeState) pElement;
    CompositeState compositeReachedState = (CompositeState) pReachedState;
    CompositePrecision compositePrecision = (CompositePrecision) pPrecision;

    List<AbstractState> compositeElements = compositeState.getWrappedStates();
    List<AbstractState> compositeReachedStates = compositeReachedState.getWrappedStates();
    List<Precision> compositePrecisions = compositePrecision.getPrecisions();

    for (int idx = 0; idx < compositeElements.size(); idx++) {
      StopOperator stopOp = stopOperators.get(idx);

      AbstractState wrappedState = compositeElements.get(idx);
      AbstractState wrappedReachedState = compositeReachedStates.get(idx);
      Precision prec = compositePrecisions.get(idx);

      boolean possible;
      if (stopOp instanceof ForcedCoveringStopOperator) {

        possible = ((ForcedCoveringStopOperator) stopOp)
            .isForcedCoveringPossible(wrappedState, wrappedReachedState, prec);

      } else {
        possible = stopOp.stop(wrappedState, Collections.singleton(wrappedReachedState), prec);
      }

      if (!possible) {
        return false;
      }
    }

    return true;
  }
}
