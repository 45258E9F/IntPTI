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
package org.sosy_lab.cpachecker.cpa.automaton;

import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.StringExpression;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a State in the automaton.
 */
public class AutomatonInternalState {
  // the StateId is used to identify States in GraphViz
  private static final UniqueIdGenerator idGenerator = new UniqueIdGenerator();
  private final int stateId = idGenerator.getFreshId();

  /**
   * State representing BOTTOM
   */
  static final AutomatonInternalState BOTTOM = new AutomatonInternalState("_predefinedState_BOTTOM",
      Collections.<AutomatonTransition>emptyList());

  /**
   * Error State
   */
  static final AutomatonInternalState ERROR = new AutomatonInternalState(
      "_predefinedState_ERROR",
      Collections.singletonList(new AutomatonTransition(
          AutomatonBoolExpr.TRUE,
          Collections.<AutomatonBoolExpr>emptyList(),
          null,
          Collections.<AutomatonAction>emptyList(),
          BOTTOM, new StringExpression(""))),
      true, false);

  /**
   * Break state, used to halt the analysis without being a target state
   */
  static final AutomatonInternalState BREAK = new AutomatonInternalState(
      "_predefinedState_BREAK",
      Collections.singletonList(new AutomatonTransition(
          AutomatonBoolExpr.TRUE,
          Collections.<AutomatonBoolExpr>emptyList(),
          null,
          Collections.<AutomatonAction>emptyList(),
          BOTTOM, null)),
      false, false);

  /**
   * Name of this State.
   */
  private final String name;
  /**
   * Outgoing transitions of this state.
   */
  private final List<AutomatonTransition> transitions;

  private final boolean mIsTarget;

  /**
   * determines if all transitions of the state are considered or only the first that matches
   */
  private final boolean mAllTransitions;

  public AutomatonInternalState(
      String pName,
      List<AutomatonTransition> pTransitions,
      boolean pIsTarget,
      boolean pAllTransitions) {
    this.name = pName;
    this.transitions = pTransitions;
    this.mIsTarget = pIsTarget;
    this.mAllTransitions = pAllTransitions;
  }

  public AutomatonInternalState(String pName, List<AutomatonTransition> pTransitions) {
    this(pName, pTransitions, false, false);
  }

  public boolean isNonDetState() {
    return mAllTransitions;
  }

  /**
   * Lets all outgoing transitions of this state resolve their "sink" states.
   *
   * @param pAllStates map of all states of this automaton.
   */
  void setFollowStates(Map<String, AutomatonInternalState> pAllStates)
      throws InvalidAutomatonException {
    for (AutomatonTransition t : transitions) {
      t.setFollowState(pAllStates);
    }
  }

  public String getName() {
    return name;
  }

  /**
   * @return a integer representation of this state.
   */
  public int getStateId() {
    return stateId;
  }

  public boolean isTarget() {
    return mIsTarget;
  }

  /**
   * @return Is it a state in that we will remain the rest of the time?
   */
  public boolean isFinalSelfLoopingState() {
    if (transitions.size() == 1) {
      AutomatonTransition tr = transitions.get(0);
      if (tr.getFollowState().equals(this)) {
        return true;
      }
    }

    return false;
  }

  public boolean getDoesMatchAll() {
    return mAllTransitions;
  }

  public List<AutomatonTransition> getTransitions() {
    return transitions;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
