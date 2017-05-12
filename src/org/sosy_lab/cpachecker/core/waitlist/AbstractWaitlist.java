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

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import java.util.Collection;
import java.util.Iterator;

/**
 * Default implementation for a waitlist that uses any collection as the backing
 * data structure. All methods except pop() are implemented by delegating them
 * to the correspondent Collection method.
 *
 * Sub-classes may choose their own collection implementation (e.g. a LinkedList
 * or an ArrayDeque) depending on their needs for pop().
 */
public abstract class AbstractWaitlist<T extends Collection<AbstractState>> implements Waitlist {

  protected final T waitlist;

  protected AbstractWaitlist(T pWaitlist) {
    waitlist = pWaitlist;
  }

  @Override
  public void add(AbstractState pStat) {
    waitlist.add(pStat);
  }

  @Override
  public void clear() {
    waitlist.clear();
  }

  @Override
  public boolean contains(AbstractState pState) {
    return waitlist.contains(pState);
  }

  @Override
  public boolean isEmpty() {
    return waitlist.isEmpty();
  }

  @Override
  public Iterator<AbstractState> iterator() {
    return waitlist.iterator();
  }

  @Override
  public boolean remove(AbstractState pState) {
    return waitlist.remove(pState);
  }

  @Override
  public int size() {
    return waitlist.size();
  }

  @Override
  public String toString() {
    return waitlist.toString();
  }
}
