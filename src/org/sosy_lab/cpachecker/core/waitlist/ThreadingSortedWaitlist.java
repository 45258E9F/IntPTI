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
import org.sosy_lab.cpachecker.cpa.threading.ThreadingState;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Waitlist implementation that sorts the abstract states depending
 * on the number of running threads (if there are any).
 * States with fewer running threads are considered first.
 * These states are expected to avoid state explosion,
 * as they have fewer successors due to the interleaving of threads.
 */
public class ThreadingSortedWaitlist extends AbstractSortedWaitlist<Integer> {

  protected ThreadingSortedWaitlist(WaitlistFactory pSecondaryStrategy) {
    super(pSecondaryStrategy);
  }

  @Override
  protected Integer getSortKey(AbstractState pState) {
    ThreadingState state =
        AbstractStates.extractStateByType(pState, ThreadingState.class);

    // negate size so that the highest key corresponds to the smallest map
    return (state == null) ? 0 : -state.getThreadIds().size();
  }

  public static WaitlistFactory factory(final WaitlistFactory pSecondaryStrategy) {
    return new WaitlistFactory() {

      @Override
      public Waitlist createWaitlistInstance() {
        return new ThreadingSortedWaitlist(pSecondaryStrategy);
      }
    };
  }
}
