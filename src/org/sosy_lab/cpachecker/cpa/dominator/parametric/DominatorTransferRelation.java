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
package org.sosy_lab.cpachecker.cpa.dominator.parametric;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DominatorTransferRelation extends SingleEdgeTransferRelation {

  private final ConfigurableProgramAnalysis cpa;

  public DominatorTransferRelation(ConfigurableProgramAnalysis cpa) {
    if (cpa == null) {
      throw new IllegalArgumentException("cpa is null!");
    }

    this.cpa = cpa;
  }

  @Override
  public Collection<DominatorState> getAbstractSuccessorsForEdge(
      AbstractState element, List<AbstractState> otherStates, Precision prec, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {

    assert element instanceof DominatorState;

    DominatorState dominatorState = (DominatorState) element;

    Collection<? extends AbstractState> successorsOfDominatedElement =
        this.cpa.getTransferRelation().getAbstractSuccessorsForEdge(
            dominatorState.getDominatedState(), otherStates, prec, cfaEdge);

    Collection<DominatorState> successors = new ArrayList<>(successorsOfDominatedElement.size());
    for (AbstractState successorOfDominatedElement : successorsOfDominatedElement) {
      DominatorState successor = new DominatorState(successorOfDominatedElement, dominatorState);
      successor.update(successorOfDominatedElement);
      successors.add(successor);
    }

    return successors;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState element,
      List<AbstractState> otherElements, CFAEdge cfaEdge,
      Precision precision) {
    return null;
  }
}
