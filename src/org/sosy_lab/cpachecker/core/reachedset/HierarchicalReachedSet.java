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
package org.sosy_lab.cpachecker.core.reachedset;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterators;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Hierarchical reached set for multi-entry analysis.
 * Reached set has a working list and a cache list, which stores states derived by the analysis
 * from the previous entry. Finally, we should merge states in the cache list into the working list.
 */
public class HierarchicalReachedSet implements ReachedSet {

  /* ********************************* */
  /* legacy reached set data structure */
  /* ********************************* */

  private final LinkedHashMap<AbstractState, Precision> workingReached;
  private final Set<AbstractState> unmodifiableReached;
  protected AbstractState lastState = null;
  private AbstractState firstState = null;
  protected final Waitlist waitlist;

  /* ******************* */
  /* hierarchical fields */
  /* ******************* */

  private final HashSet<CFANode> waitEntry;
  private final HashSet<CFANode> usedEntry;

  HierarchicalReachedSet(WaitlistFactory pWaitlistFactory) {
    workingReached = new LinkedHashMap<>();
    unmodifiableReached = Collections.unmodifiableSet(workingReached.keySet());
    waitlist = pWaitlistFactory.createWaitlistInstance();
    waitEntry = new HashSet<>();
    usedEntry = new HashSet<>();
  }

  /* ***************** */
  /* modifying methods */
  /* ***************** */

  @Override
  public void add(AbstractState state, Precision precision) throws IllegalArgumentException {
    Preconditions.checkNotNull(state);
    Preconditions.checkNotNull(precision);
    if (workingReached.isEmpty()) {
      firstState = state;
    }
    Precision prevPrecision = workingReached.put(state, precision);
    if (prevPrecision == null) {
      waitlist.add(state);
      lastState = state;
    } else {
      if (!precision.equals(prevPrecision)) {
        workingReached.put(state, prevPrecision);
        throw new IllegalArgumentException("State to be added already exists, but with a "
            + "different precision");
      }
    }
  }

  @Override
  public void addAll(Iterable<Pair<AbstractState, Precision>> toAdd) {
    for (Pair<AbstractState, Precision> pair : toAdd) {
      add(pair.getFirstNotNull(), pair.getSecondNotNull());
    }
  }

  @Override
  public void reAddToWaitlist(AbstractState s) {
    Preconditions.checkNotNull(s);
    Preconditions.checkArgument(workingReached.containsKey(s), "State has to be in the reached "
        + "set");
    if (!waitlist.contains(s)) {
      waitlist.add(s);
    }
  }

  @Override
  public void updatePrecision(
      AbstractState s, Precision newPrecision) {
    Preconditions.checkNotNull(s);
    Preconditions.checkNotNull(newPrecision);
    Precision oldPrecision = workingReached.put(s, newPrecision);
    if (oldPrecision == null) {
      workingReached.remove(s);
      throw new IllegalArgumentException("State has to be in the reached set");
    }
  }

  @Override
  public void remove(AbstractState state) {
    Preconditions.checkNotNull(state);
    int hash = state.hashCode();
    if (firstState != null && hash == firstState.hashCode() && state.equals(firstState)) {
      firstState = null;
    }
    if (lastState != null && hash == lastState.hashCode() && state.equals(lastState)) {
      lastState = null;
    }
    //noinspection ResultOfMethodCallIgnored
    waitlist.remove(state);
    workingReached.remove(state);
  }

  @Override
  public void removeAll(Iterable<? extends AbstractState> toRemove) {
    for (AbstractState state : toRemove) {
      remove(state);
    }
  }

  @Override
  public void removeOnlyFromWaitlist(AbstractState state) {
    Preconditions.checkNotNull(state);
    //noinspection ResultOfMethodCallIgnored
    waitlist.remove(state);
  }

  @Override
  public void clear() {
    firstState = null;
    lastState = null;
    waitlist.clear();
    workingReached.clear();
  }

  /* ********************* */
  /* non-modifying methods */
  /* ********************* */

  @Override
  public Set<AbstractState> asCollection() {
    return unmodifiableReached;
  }

  @Override
  public Iterator<AbstractState> iterator() {
    return unmodifiableReached.iterator();
  }

  @Override
  public Collection<Precision> getPrecisions() {
    return Collections.unmodifiableCollection(workingReached.values());
  }

  @Override
  public Collection<AbstractState> getReached(AbstractState state)
      throws UnsupportedOperationException {
    return asCollection();
  }

  @Override
  public Collection<AbstractState> getReached(CFANode location) {
    return asCollection();
  }

  @Override
  public AbstractState getFirstState() {
    Preconditions.checkState(firstState != null);
    return firstState;
  }


  @Override
  public AbstractState getLastState() {
    Preconditions.checkState(lastState != null);
    return lastState;
  }

  @Override
  public boolean hasWaitingState() {
    return !waitlist.isEmpty();
  }

  @Override
  public Collection<AbstractState> getWaitlist() {
    return new AbstractCollection<AbstractState>() {
      @Override
      public Iterator<AbstractState> iterator() {
        return Iterators.unmodifiableIterator(waitlist.iterator());
      }

      @Override
      public int size() {
        return waitlist.size();
      }

      @Override
      public boolean contains(Object o) {
        return o instanceof AbstractState && waitlist.contains((AbstractState) o);
      }

      @Override
      public boolean isEmpty() {
        return waitlist.isEmpty();
      }

      @Override
      public String toString() {
        return waitlist.toString();
      }
    };
  }

  @Override
  public AbstractState popFromWaitlist() {
    return waitlist.pop();
  }

  @Override
  public Precision getPrecision(AbstractState state) {
    Preconditions.checkNotNull(state);
    Precision precision = workingReached.get(state);
    Preconditions.checkArgument(precision != null, "State not in the reached state:\n%s", state);
    return precision;
  }

  @Override
  public <T extends AbstractState> boolean contains(AbstractState s, Class<T> type) {
    Preconditions.checkArgument(s.getClass().equals(type), "Type mismatch");
    return FluentIterable.from(unmodifiableReached).transform(AbstractStates.toState(type))
        .contains(s);
  }

  @Override
  public boolean contains(AbstractState state) {
    Preconditions.checkNotNull(state);
    return workingReached.containsKey(state);
  }

  @Override
  public int size() {
    return workingReached.size();
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public String toString() {
    return unmodifiableReached.toString();
  }

  /* ********************** */
  /* hierarchical operation */
  /* ********************** */

  public void pushEntry(CFANode pNode) {
    if (!usedEntry.contains(pNode)) {
      waitEntry.add(pNode);
    }
  }

  public void pushEntries(Collection<CFANode> pNodes) {
    for (CFANode pNode : pNodes) {
      pushEntry(pNode);
    }
  }

  public boolean hasWaitingEntry() {
    return !waitEntry.isEmpty();
  }

  public int sizeUsedEntry() {
    return usedEntry.size();
  }

  public int sizeWaitEntry() {
    return waitEntry.size();
  }

  public CFANode peekEntry() {
    return waitEntry.isEmpty() ? null : waitEntry.iterator().next();
  }

  @Nullable
  public CFANode popEntry() {
    Iterator<CFANode> iterator = waitEntry.iterator();
    if (iterator.hasNext()) {
      CFANode entry = iterator.next();
      waitEntry.remove(entry);
      usedEntry.add(entry);
      return entry;
    } else {
      return null;
    }
  }

  /**
   * Reinitialize reached set with the specified abstract state and precision.
   * Precondition: waitlist should be empty
   */
  public void reInitialize(Collection<AbstractState> pStates, Precision pPrecision) {
    Preconditions.checkArgument(waitlist.isEmpty(), "Waitlist required to be empty");
    workingReached.clear();
    firstState = null;
    lastState = null;
    for (AbstractState state : pStates) {
      add(state, pPrecision);
    }
  }

  /**
   * Summarize cached reached states.
   * For memory efficiency, we do not keep the history reached states.
   */
  public void summarize() {
    if (!waitlist.isEmpty()) {
      waitlist.clear();
    }
    workingReached.clear();
  }

  /**
   * Clear the waitlist when an error occurs and the analysis is set to be interrupted.
   */
  public void dropWaitingStates() {
    waitlist.clear();
  }

}
