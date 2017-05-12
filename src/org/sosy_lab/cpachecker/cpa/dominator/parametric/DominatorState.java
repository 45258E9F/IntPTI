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
package org.sosy_lab.cpachecker.cpa.dominator.parametric;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class DominatorState implements AbstractStateWithLocation, AbstractState {

  private AbstractState dominatedState;
  private Set<AbstractState> dominators = new HashSet<>();

  public DominatorState(AbstractState dominatedElement) {
    if (dominatedElement == null) {
      throw new IllegalArgumentException("dominatedState is null!");
    }

    this.dominatedState = dominatedElement;
  }

  public DominatorState(AbstractState dominatedElement, Set<AbstractState> dominators) {
    this(dominatedElement);

    if (dominators == null) {
      throw new IllegalArgumentException("dominators is null!");
    }

    this.dominators.addAll(dominators);
  }

  protected DominatorState() {
    dominatedState = null;
  }

  public DominatorState(DominatorState other) {
    this(other.dominatedState, other.dominators);
  }

  public DominatorState(AbstractState dominatedElement, DominatorState other) {
    this(dominatedElement, other.dominators);
  }

  public void update(AbstractState dominator) {
    if (dominator == null) {
      throw new IllegalArgumentException("dominator is null!");
    }

    dominators.add(dominator);
  }

  public AbstractState getDominatedState() {
    return this.dominatedState;
  }

  public Iterator<AbstractState> getIterator() {
    return this.dominators.iterator();
  }

  public boolean isDominatedBy(AbstractState dominator) {
    return this.dominators.contains(dominator);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof DominatorState)) {
      return false;
    }

    DominatorState other_element = (DominatorState) other;

    if (!(this.dominatedState.equals(other_element.dominatedState))) {
      return false;
    }

    if (dominators.size() != other_element.dominators.size()) {
      return false;
    }

    for (AbstractState dominator : dominators) {
      if (!other_element.isDominatedBy(dominator)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("( " + this.dominatedState.toString() + ", {");

    boolean first = true;
    for (AbstractState dominator : this.dominators) {
      if (first) {
        first = false;
      } else {
        builder.append(", ");
      }

      builder.append(dominator.toString());
    }

    builder.append("})");

    return builder.toString();
  }

  @Override
  public CFANode getLocationNode() {
    return ((AbstractStateWithLocation) dominatedState).getLocationNode();
  }

  @Override
  public Iterable<CFANode> getLocationNodes() {
    return ((AbstractStateWithLocation) dominatedState).getLocationNodes();
  }

  @Override
  public Iterable<CFAEdge> getOutgoingEdges() {
    return ((AbstractStateWithLocations) dominatedState).getOutgoingEdges();
  }

  @Override
  public int hashCode() {
    // TODO: create better hash code?
    return this.dominatedState.hashCode();
  }
}
