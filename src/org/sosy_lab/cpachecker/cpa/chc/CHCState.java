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
package org.sosy_lab.cpachecker.cpa.chc;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class CHCState implements AbstractState {

  /*
   * store the node number: needed to build an ancestor tree
   */
  int nodeNumber;

  /*
   *  symbolic representation of the set of concrete
   *  states approximated by this abstract state
   */
  Constraint constraint = null;

  /*
   * abstract state from which this state has been
   * derived by using the transfer relation
   */
  CHCState ancestor = null;

  /*
   * the state associated with the program point of
   * the function call statement
   */
  CHCState caller = null;


  public CHCState() {
    nodeNumber = 0;
    constraint = new Constraint();
    caller = this;
  }

  public CHCState(int nodeId, Constraint constraint) {
    this.nodeNumber = nodeId;
    this.constraint = constraint;
    caller = this;
  }

  /*
   * Copy constructor
   */
  public CHCState(CHCState crState) {
    nodeNumber = crState.nodeNumber;
    constraint = new Constraint(crState.getConstraint());
    ancestor = crState;
    caller = crState.getCaller();
  }

  public void setNodeNumber(int nodeId) {
    this.nodeNumber = nodeId;
  }

  public void setConstraint(Constraint constraint) {
    this.constraint = constraint;
  }

  public void setAncestror(CHCState ancestor) {
    this.ancestor = ancestor;
  }

  public void setCaller(CHCState caller) {
    this.caller = caller;
  }

  public int getNodeId() {
    return nodeNumber;
  }

  public Constraint getConstraint() {
    return constraint;
  }

  public CHCState getAncestor() {
    return ancestor;
  }

  public CHCState getCaller() {
    return caller;
  }

  public void updateConstraint(Constraint cns) {
    constraint = ConstraintManager.and(constraint, cns);
  }

  public void addConstraint(Constraint cns) {
    constraint.and(cns);
  }

  public void join(CHCState state1) {
    constraint = ConstraintManager.convexHull(this.constraint, state1.getConstraint());
  }

  public boolean isBottom() {
    if (constraint.isFalse()) {
      return true;
    }
    return false;
  }

  @Override
  public String toString() {
    return constraint.toString();
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }
}
