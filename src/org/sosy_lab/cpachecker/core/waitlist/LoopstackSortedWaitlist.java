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
import org.sosy_lab.cpachecker.cpa.loopstack.LoopstackState;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Waitlist implementation that sorts the abstract states by the depth of
 * their loopstack.
 * States with a larger/smaller (depending on the used factory method)
 * loopstack are considered first.
 */
public class LoopstackSortedWaitlist extends AbstractSortedWaitlist<Integer> {
  private final int multiplier;

  private LoopstackSortedWaitlist(
      WaitlistFactory pSecondaryStrategy,
      int pMultiplier) {
    super(pSecondaryStrategy);
    multiplier = pMultiplier;
  }

  @Override
  protected Integer getSortKey(AbstractState pState) {
    LoopstackState loopstackState =
        AbstractStates.extractStateByType(pState, LoopstackState.class);
    return (loopstackState != null) ? (multiplier * loopstackState.getDepth()) : 0;
  }

  public static WaitlistFactory factory(final WaitlistFactory pSecondaryStrategy) {
    return new WaitlistFactory() {
      @Override
      public Waitlist createWaitlistInstance() {
        return new LoopstackSortedWaitlist(pSecondaryStrategy, 1);
      }
    };
  }

  public static WaitlistFactory reversedFactory(
      final WaitlistFactory pSecondaryStrategy) {
    return new WaitlistFactory() {
      @Override
      public Waitlist createWaitlistInstance() {
        return new LoopstackSortedWaitlist(pSecondaryStrategy, -1);
      }
    };
  }
}
