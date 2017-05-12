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
package org.sosy_lab.cpachecker.core.reachedset;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

import java.util.Collection;
import java.util.Iterator;

/**
 * Interface representing an unmodifiable reached set
 */
public interface UnmodifiableReachedSet extends Iterable<AbstractState> {

  public Collection<AbstractState> asCollection();

  @Override
  public Iterator<AbstractState> iterator();

  public Collection<Precision> getPrecisions();

  /**
   * Returns a subset of the reached set, which contains at least all abstract
   * states belonging to the same location as a given state. It may even
   * return an empty set if there are no such states. Note that it may return up to
   * all abstract states.
   *
   * The returned set is a view of the actual data, so it might change if nodes
   * are added to the reached set. Subsequent calls to this method with the same
   * parameter value will always return the same object.
   *
   * The returned set is unmodifiable.
   *
   * @param state An abstract state for whose location the abstract states should be retrieved.
   * @return A subset of the reached set.
   */
  public Collection<AbstractState> getReached(AbstractState state)
      throws UnsupportedOperationException;

  /**
   * Returns a subset of the reached set, which contains at least all abstract
   * states belonging to given location. It may even
   * return an empty set if there are no such states. Note that it may return up to
   * all abstract states.
   *
   * The returned set is a view of the actual data, so it might change if nodes
   * are added to the reached set. Subsequent calls to this method with the same
   * parameter value will always return the same object.
   *
   * The returned set is unmodifiable.
   *
   * @param location A location
   * @return A subset of the reached set.
   */
  public Collection<AbstractState> getReached(CFANode location);

  /**
   * Returns the first state that was added to the reached set.
   *
   * @throws IllegalStateException If the reached set is empty.
   */
  public AbstractState getFirstState();

  /**
   * Returns the last state that was added to the reached set.
   * May be null if it is unknown, which state was added last.
   */
  public AbstractState getLastState();

  public boolean hasWaitingState();

  /**
   * An unmodifiable view of the waitlist as an Collection.
   */
  public Collection<AbstractState> getWaitlist();

  /**
   * Returns the precision for a state.
   *
   * @param state The state to look for. Has to be in the reached set.
   * @return The precision for the state.
   * @throws IllegalArgumentException If the state is not in the reached set.
   */
  public Precision getPrecision(AbstractState state)
      throws UnsupportedOperationException;


  public boolean contains(AbstractState state);

  public boolean isEmpty();

  public int size();
}