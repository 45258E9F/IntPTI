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
package org.sosy_lab.cpachecker.core.waitlist;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.util.AbstractStates;


public class AutomatonMatchesWaitlist extends AbstractSortedWaitlist<Integer> {

  protected AutomatonMatchesWaitlist(WaitlistFactory pSecondaryStrategy) {
    super(pSecondaryStrategy);
  }

  @Override
  protected Integer getSortKey(AbstractState pState) {
    int sortKey = 0;
    for (AutomatonState automatonState : AbstractStates.asIterable(pState)
        .filter(AutomatonState.class)) {
      sortKey = Math.max(sortKey, automatonState.getMatches());
    }

    return sortKey;
  }

  public static WaitlistFactory factory(final WaitlistFactory pSecondaryStrategy) {
    return new WaitlistFactory() {

      @Override
      public Waitlist createWaitlistInstance() {
        return new AutomatonMatchesWaitlist(pSecondaryStrategy);
      }
    };
  }
}
