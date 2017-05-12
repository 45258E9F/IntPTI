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
package org.sosy_lab.cpachecker.core.waitlist;

import com.google.errorprone.annotations.ForOverride;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public abstract class AbstractControlledWaitlist<K extends Comparable<K>>
    extends AbstractSortedWaitlist<K> implements Waitlist {

  /**
   * The minimum allowed size of waiting states. We use this user-specified parameter to control
   * the path space.
   */
  protected final int minAllowedSize;

  protected AbstractControlledWaitlist(WaitlistFactory pSecondStrategy, int pMinSize) {
    super(pSecondStrategy);
    minAllowedSize = pMinSize;
  }

  @ForOverride
  protected abstract K getSortKey(AbstractState pState);

  @Override
  public AbstractState pop() {
    Entry<K, Waitlist> highestEntry = waitlist.lastEntry();
    Waitlist localWaitlist = highestEntry.getValue();
    assert !localWaitlist.isEmpty();
    int waitSize = localWaitlist.size();
    if (minAllowedSize > 0 && waitSize > minAllowedSize) {
      // we trim this waitlist using heuristics
      List<AbstractState> states = new ArrayList<>();
      while (!localWaitlist.isEmpty()) {
        states.add(localWaitlist.pop());
      }
      int start = waitSize % minAllowedSize;
      int step = waitSize / minAllowedSize;
      assert (step > 0);
      for (int i = start; i < waitSize; i = i + step) {
        localWaitlist.add(states.get(i));
      }
      size = size - (waitSize - minAllowedSize);
    }
    // OK, then we try to pop a state normally
    AbstractState result = localWaitlist.pop();
    if (localWaitlist.isEmpty()) {
      waitlist.remove(highestEntry.getKey());
    }
    size--;
    return result;
  }

}
