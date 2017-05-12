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
package org.sosy_lab.cpachecker.util.globalinfo;

import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonInternalState;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;

import java.util.HashMap;
import java.util.Map;


public class AutomatonInfo {
  private final Map<Integer, AutomatonInternalState> idToState;
  private final Map<String, ControlAutomatonCPA> nameToCPA;

  AutomatonInfo() {
    idToState = new HashMap<>();
    nameToCPA = new HashMap<>();
  }

  public void register(Automaton automaton, ControlAutomatonCPA cpa) {
    for (AutomatonInternalState state : automaton.getStates()) {
      idToState.put(state.getStateId(), state);
    }
    nameToCPA.put(automaton.getName(), cpa);
  }

  public AutomatonInternalState getStateById(int id) {
    return idToState.get(id);
  }

  public ControlAutomatonCPA getCPAForAutomaton(String automatonName) {
    return nameToCPA.get(automatonName);
  }
}
