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

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.ForOverride;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Default implementation of a sorted waitlist.
 * The key that is used for sorting is defined by sub-classes (it's type is
 * the type parameter of this class).
 *
 * There may be several abstract states with the same key, so this class
 * delegates the decision which of those should be chosen to a second waitlist
 * implementation. A factory for this implementation needs to be given to the
 * constructor.
 *
 * The iterators created by this class are unmodifiable.
 */
public abstract class AbstractSortedWaitlist<K extends Comparable<K>> implements Waitlist {

  private final WaitlistFactory wrappedWaitlist;

  // invariant: all entries in this map are non-empty
  protected final NavigableMap<K, Waitlist> waitlist = new TreeMap<>();

  protected int size = 0;

  /**
   * Constructor that needs a factory for the waitlist implementation that
   * should be used to store states with the same sorting key.
   */
  protected AbstractSortedWaitlist(WaitlistFactory pSecondaryStrategy) {
    wrappedWaitlist = Preconditions.checkNotNull(pSecondaryStrategy);
  }

  /**
   * Method that generates the sorting key for any abstract state.
   * States with largest key are considered first.
   * This method may not return null.
   * If this method throws an exception, no guarantees about the state of the
   * current instance of this class are made.
   */
  @ForOverride
  protected abstract K getSortKey(AbstractState pState);

  @Override
  public void add(AbstractState pState) {
    K key = getSortKey(pState);
    Waitlist localWaitlist = waitlist.get(key);
    if (localWaitlist == null) {
      localWaitlist = wrappedWaitlist.createWaitlistInstance();
      waitlist.put(key, localWaitlist);
    } else {
      assert !localWaitlist.isEmpty();
    }
    localWaitlist.add(pState);
    size++;
  }

  @Override
  public boolean contains(AbstractState pState) {
    K key = getSortKey(pState);
    Waitlist localWaitlist = waitlist.get(key);
    if (localWaitlist == null) {
      return false;
    }
    assert !localWaitlist.isEmpty();
    return localWaitlist.contains(pState);
  }

  @Override
  public void clear() {
    waitlist.clear();
    size = 0;
  }

  @Override
  public boolean isEmpty() {
    assert waitlist.isEmpty() == (size == 0);
    return waitlist.isEmpty();
  }

  @Override
  public Iterator<AbstractState> iterator() {
    return Iterables.concat(waitlist.values()).iterator();
  }

  @Override
  public AbstractState pop() {
    Entry<K, Waitlist> highestEntry;
    highestEntry = waitlist.lastEntry();
    Waitlist localWaitlist = highestEntry.getValue();
    assert !localWaitlist.isEmpty();
    AbstractState result = localWaitlist.pop();
    if (localWaitlist.isEmpty()) {
      waitlist.remove(highestEntry.getKey());
    }
    size--;
    return result;
  }

  @Override
  public boolean remove(AbstractState pState) {
    K key = getSortKey(pState);
    Waitlist localWaitlist = waitlist.get(key);
    if (localWaitlist == null) {
      return false;
    }
    assert !localWaitlist.isEmpty();
    boolean result = localWaitlist.remove(pState);
    if (result) {
      if (localWaitlist.isEmpty()) {
        waitlist.remove(key);
      }
      size--;
    }
    return result;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public String toString() {
    return waitlist.toString();
  }
}
