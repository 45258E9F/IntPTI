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

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class DominatorDomain implements AbstractDomain {

  private final ConfigurableProgramAnalysis cpa;

  public DominatorDomain(ConfigurableProgramAnalysis cpa) {
    this.cpa = cpa;
  }

  private static class DominatorTopState extends DominatorState {

    @Override
    public String toString() {
      return "\\bot";
    }

    @Override
    public boolean equals(Object o) {
      return (o instanceof DominatorTopState);
    }

    @Override
    public int hashCode() {
      return Integer.MIN_VALUE;
    }

    @Override
    public CFANode getLocationNode() {
      // TODO Auto-generated method stub
      return null;
    }
  }

  private final static DominatorTopState topState = new DominatorTopState();

  @Override
  public boolean isLessOrEqual(AbstractState element1, AbstractState element2)
      throws CPAException, InterruptedException {
    if (element1.equals(element2)) {
      return true;
    }

    if (element2.equals(topState)) {
      return true;
    }

    if (element1 instanceof DominatorState && element2 instanceof DominatorState) {
      DominatorState dominatorState1 = (DominatorState) element1;
      DominatorState dominatorState2 = (DominatorState) element2;

      if (this.cpa.getAbstractDomain().isLessOrEqual(dominatorState1.getDominatedState(),
          dominatorState2.getDominatedState())) {
        Iterator<AbstractState> dominatorIterator = dominatorState2.getIterator();

        while (dominatorIterator.hasNext()) {
          AbstractState dominator = dominatorIterator.next();

          if (!dominatorState1.isDominatedBy(dominator)) {
            return false;
          }
        }

        return true;
      }
    }

    return false;
  }

  @Override
  public AbstractState join(AbstractState element1, AbstractState element2) {
    if (!(element1 instanceof DominatorState)) {
      throw new IllegalArgumentException(
          "element1 is not a DominatorState!");
    }

    if (!(element2 instanceof DominatorState)) {
      throw new IllegalArgumentException(
          "element2 is not a DominatorState!");
    }

    DominatorState dominatorState1 = (DominatorState) element1;
    DominatorState dominatorState2 = (DominatorState) element2;

    if (element1.equals(topState)) {
      return dominatorState1;
    }

    if (element2.equals(topState)) {
      return dominatorState2;
    }

    if (!dominatorState1.getDominatedState().equals(dominatorState2.getDominatedState())) {
      return topState;
    }

    Set<AbstractState> intersectingDominators = new HashSet<>();

    Iterator<AbstractState> dominatorIterator = dominatorState1.getIterator();

    while (dominatorIterator.hasNext()) {
      AbstractState dominator = dominatorIterator.next();

      if (dominatorState2.isDominatedBy(dominator)) {
        intersectingDominators.add(dominator);
      }
    }

    DominatorState result =
        new DominatorState(dominatorState1.getDominatedState(), intersectingDominators);

    result.update(dominatorState1.getDominatedState());

    return result;
  }
}
