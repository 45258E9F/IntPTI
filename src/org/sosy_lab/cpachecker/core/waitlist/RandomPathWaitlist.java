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

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.util.LinkedList;
import java.util.Random;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Waitlist that implements DFS behavior with random selection of branching path.
 *
 * pop() removes the last added state of the path that is currently explored (DFS behavior).
 * If the last iteration added more than one state (branching case of successor computation) pop()
 * returns one of these successors at random.
 */
@SuppressFBWarnings(value = "BC_BAD_CAST_TO_CONCRETE_COLLECTION",
    justification = "warnings is only because of casts introduced by generics")
public class RandomPathWaitlist extends AbstractWaitlist<LinkedList<AbstractState>> {

  private final Random rand = new Random();
  private int successorsOfParent;
  private CFANode parent;

  protected RandomPathWaitlist() {
    super(new LinkedList<AbstractState>());
    successorsOfParent = 0;
  }

  @Override
  public void add(AbstractState pStat) {
    super.add(pStat);
    CFANode location = AbstractStates.extractLocation(pStat);
    if (parent == null || (!parent.hasEdgeTo(location))) {
      parent = location;
      successorsOfParent = 0;
    } else {
      successorsOfParent++;
    }
  }


  @Override
  public AbstractState pop() {
    AbstractState state;
    if (waitlist.size() < 2 || successorsOfParent < 2) {
      state = waitlist.getLast();
    } else {
      // successorsOnLevelCount >= 2
      int r = rand.nextInt(successorsOfParent) + 1;
      state = waitlist.get(waitlist.size() - r);
    }
    if (successorsOfParent > 0) {
      successorsOfParent--;
      parent = AbstractStates.extractLocation(state);
    } else {
      parent = null;//TODO not sure if a reset to no parent is correct.
    }
    return state;
  }
}
