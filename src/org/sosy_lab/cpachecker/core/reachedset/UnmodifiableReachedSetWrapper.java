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

import com.google.common.collect.Iterators;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public class UnmodifiableReachedSetWrapper implements UnmodifiableReachedSet {

  private final UnmodifiableReachedSet delegate;

  public UnmodifiableReachedSetWrapper(UnmodifiableReachedSet pDelegate) {
    delegate = pDelegate;
  }

  @Override
  public Collection<AbstractState> asCollection() {
    return Collections.unmodifiableCollection(delegate.asCollection());
  }

  @Override
  public Iterator<AbstractState> iterator() {
    return Iterators.unmodifiableIterator(delegate.iterator());
  }

  @Override
  public Collection<Precision> getPrecisions() {
    return Collections.unmodifiableCollection(delegate.getPrecisions());
  }

  @Override
  public Collection<AbstractState> getReached(AbstractState pState)
      throws UnsupportedOperationException {
    return Collections.unmodifiableCollection(delegate.getReached(pState));
  }

  @Override
  public Collection<AbstractState> getReached(CFANode pLocation) {
    return Collections.unmodifiableCollection(delegate.getReached(pLocation));
  }

  @Override
  public AbstractState getFirstState() {
    return delegate.getFirstState();
  }

  @Override
  public AbstractState getLastState() {
    return delegate.getLastState();
  }

  @Override
  public boolean hasWaitingState() {
    return delegate.hasWaitingState();
  }

  @Override
  public Collection<AbstractState> getWaitlist() {
    return Collections.unmodifiableCollection(delegate.getWaitlist());
  }

  @Override
  public Precision getPrecision(AbstractState pState)
      throws UnsupportedOperationException {
    return delegate.getPrecision(pState);
  }

  @Override
  public boolean contains(AbstractState pState) {
    return delegate.contains(pState);
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
