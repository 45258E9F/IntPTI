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
package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;


public class Automata {

  private static final Automaton LOOP_HEAD_TARGET_AUTOMATON;

  static {
    String initStateName = "Init";
    AutomatonTransition toInit = new AutomatonTransition(
        AutomatonBoolExpr.TRUE,
        Collections.<AutomatonBoolExpr>emptyList(),
        Collections.<AutomatonAction>emptyList(),
        initStateName);

    AutomatonInternalState targetState = AutomatonInternalState.ERROR;
    AutomatonTransition toTarget = new AutomatonTransition(
        AutomatonBoolExpr.MatchLoopStart.INSTANCE,
        Collections.<AutomatonBoolExpr>emptyList(),
        Collections.<AutomatonAction>emptyList(),
        targetState);

    AutomatonInternalState initState =
        new AutomatonInternalState(initStateName, Lists.newArrayList(toInit, toTarget), false,
            true);

    List<AutomatonInternalState> states = Lists.newArrayList(initState, targetState);

    try {
      LOOP_HEAD_TARGET_AUTOMATON = new Automaton(
          "LoopHeadTarget",
          Collections.<String, AutomatonVariable>emptyMap(),
          states,
          initStateName);
    } catch (InvalidAutomatonException e) {
      throw new AssertionError("Automaton built in code should be valid.");
    }
  }

  public static Automaton getLoopHeadTargetAutomaton() {
    return LOOP_HEAD_TARGET_AUTOMATON;
  }

  private Automata() {

  }

}
