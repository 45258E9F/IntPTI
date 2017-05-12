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

import com.google.common.base.Objects;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.waitlist.DominationSortedWaitlist.DominationKey;
import org.sosy_lab.cpachecker.cpa.boundary.BoundaryState;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class DominationSortedWaitlist extends AbstractControlledWaitlist<DominationKey> {

  public static class DominationKey implements Comparable<DominationKey> {

    int dominateKey;
    int callDepth;

    private DominationKey(int pDominateKey, int pCallDepth) {
      dominateKey = pDominateKey;
      callDepth = pCallDepth;
    }

    static DominationKey of(int pDominateKey, int pCallDepth) {
      return new DominationKey(pDominateKey, pCallDepth);
    }

    @Override
    public int compareTo(DominationKey that) {
      int result = Integer.signum(callDepth - that.callDepth);
      return (result != 0) ? result : Integer.signum(dominateKey - that.dominateKey);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(dominateKey, callDepth);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || !(obj instanceof DominationKey)) {
        return false;
      }
      DominationKey that = (DominationKey) obj;
      return dominateKey == that.dominateKey && callDepth == that.callDepth;
    }
  }

  private DominationSortedWaitlist(WaitlistFactory pSecondStrategy, int maxWaitSize) {
    super(pSecondStrategy, maxWaitSize);
  }

  @Override
  public void add(AbstractState pState) {
    assert AbstractStates.extractLocation(pState) != null;
    super.add(pState);
  }

  @Override
  protected DominationKey getSortKey(AbstractState pState) {
    BoundaryState boundary = AbstractStates.extractStateByType(pState, BoundaryState.class);
    int callDepth;
    if (boundary != null) {
      callDepth = boundary.getCallDepth();
    } else {
      // otherwise, we do not care about the call stack level
      callDepth = 1;
    }
    CFANode node = AbstractStates.extractLocation(pState);
    assert (node != null);
    int domKey = node.getPostDominatorId();
    return DominationKey.of(domKey, callDepth);
  }

  @Override
  public final AbstractState pop() {
    // Here we employed a dynamic heuristic to control the threshold on waitlist size.
    Entry<DominationKey, Waitlist> highestEntry = waitlist.lastEntry();
    DominationKey key = highestEntry.getKey();
    Waitlist localWaitlist = highestEntry.getValue();
    assert (!localWaitlist.isEmpty());
    int waitSize = localWaitlist.size();
    if (minAllowedSize > 0) {
      double maxWaitSizeDbl = minAllowedSize;
      for (int i = 2; i <= key.callDepth; i++) {
        maxWaitSizeDbl = Math.sqrt(maxWaitSizeDbl);
      }
      int maxWaitSize = (int) Math.floor(maxWaitSizeDbl);
      if (maxWaitSize < 1) {
        maxWaitSize = 1;
      }
      if (waitSize > maxWaitSize) {
        List<AbstractState> states = new ArrayList<>();
        while (!localWaitlist.isEmpty()) {
          states.add(localWaitlist.pop());
        }
        int start = waitSize % maxWaitSize;
        int step = waitSize / maxWaitSize;
        assert (step > 0);
        for (int i = start; i < waitSize; i = i + step) {
          localWaitlist.add(states.get(i));
        }
        size = size - (waitSize - maxWaitSize);
      }
    }
    AbstractState result = localWaitlist.pop();
    if (localWaitlist.isEmpty()) {
      waitlist.remove(key);
    }
    size--;
    return result;
  }

  public static WaitlistFactory factory(
      final WaitlistFactory pSecondStrategy, final int
      maxWaitSize) {
    return new WaitlistFactory() {
      @Override
      public Waitlist createWaitlistInstance() {
        return new DominationSortedWaitlist(pSecondStrategy, maxWaitSize);
      }
    };
  }

}
