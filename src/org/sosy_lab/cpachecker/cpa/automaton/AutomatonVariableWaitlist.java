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

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.waitlist.AbstractSortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class AutomatonVariableWaitlist extends AbstractSortedWaitlist<Integer> {

  private final String variableId;

  private AutomatonVariableWaitlist(WaitlistFactory pSecondaryStrategy, String pVariableId) {
    super(pSecondaryStrategy);
    this.variableId = pVariableId;
  }

  @Override
  protected Integer getSortKey(AbstractState pState) {
    int sortKey = Integer.MIN_VALUE;
    for (AutomatonState automatonState : AbstractStates.asIterable(pState)
        .filter(AutomatonState.class)) {
      AutomatonVariable variable = automatonState.getVars().get(variableId);
      if (variable != null) {
        sortKey = Math.max(sortKey, variable.getValue());
      }
    }

    return sortKey;
  }

  public static WaitlistFactory factory(
      final WaitlistFactory pSecondaryStrategy,
      final String pVariableId) {
    return new WaitlistFactory() {

      @Override
      public Waitlist createWaitlistInstance() {
        return new AutomatonVariableWaitlist(pSecondaryStrategy, pVariableId);
      }
    };
  }
}
