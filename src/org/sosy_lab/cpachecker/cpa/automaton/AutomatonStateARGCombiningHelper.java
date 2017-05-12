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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Map;


public class AutomatonStateARGCombiningHelper {

  private final Map<String, AutomatonInternalState> qualifiedAutomatonStateNameToInternalState;
  private final Map<String, ControlAutomatonCPA> nameToCPA;

  public AutomatonStateARGCombiningHelper() {
    qualifiedAutomatonStateNameToInternalState = Maps.newHashMap();
    nameToCPA = Maps.newHashMap();
  }

  public boolean registerAutomaton(final AutomatonState pStateOfAutomata) {
    ControlAutomatonCPA automatonCPA = pStateOfAutomata.getAutomatonCPA();
    final String prefix = automatonCPA.getAutomaton().getName() + "::";
    String qualifiedName;

    if (nameToCPA.put(automatonCPA.getAutomaton().getName(), automatonCPA) != null) {
      return false;
    }

    for (AutomatonInternalState internal : automatonCPA.getAutomaton().getStates()) {
      qualifiedName = prefix + internal.getName();
      if (qualifiedAutomatonStateNameToInternalState.put(qualifiedName, internal) != null) {
        return false;
      }
    }

    return true;
  }

  public AutomatonState replaceStateByStateInAutomatonOfSameInstance(final AutomatonState toReplace)
      throws CPAException {
    String qualifiedName =
        toReplace.getOwningAutomatonName() + "::" + toReplace.getInternalStateName();

    if (qualifiedAutomatonStateNameToInternalState.containsKey(qualifiedName)) {
      AutomatonSafetyProperty violatedProp = null;

      if (toReplace.getViolatedProperties().size() > 0) {
        Property prop = toReplace.getViolatedProperties().iterator().next();
        assert prop instanceof AutomatonSafetyProperty;
        violatedProp = (AutomatonSafetyProperty) prop;
      }

      return AutomatonState.automatonStateFactory(
          toReplace.getVars(),
          qualifiedAutomatonStateNameToInternalState.get(qualifiedName),
          nameToCPA.get(toReplace.getOwningAutomatonName()),
          toReplace.getAssumptions(),
          toReplace.getCandidateInvariants(),
          toReplace.getMatches(),
          toReplace.getFailedMatches(),
          violatedProp);
    }

    throw new CPAException("Changing state failed, unknown state.");
  }

  public boolean considersAutomaton(final String pAutomatonName) {
    return nameToCPA.containsKey(pAutomatonName);
  }

  public static boolean endsInAssumptionTrueState(
      final AutomatonState pPredecessor,
      final CFAEdge pEdge) {
    Preconditions.checkNotNull(pPredecessor);
    try {
      for (AbstractState successor : pPredecessor.getAutomatonCPA().getTransferRelation()
          .getAbstractSuccessorsForEdge(pPredecessor, Lists.<AbstractState>newArrayList(),
              SingletonPrecision.getInstance(), pEdge)) {
        if (!((AutomatonState) successor).getInternalStateName().equals("__TRUE")) {
          return false;
        }
      }
    } catch (CPATransferException e) {
      return false;
    }
    return true;
  }

}
