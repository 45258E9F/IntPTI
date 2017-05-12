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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

import java.util.Collection;
import java.util.Iterator;

/**
 * Live view of an unmodifiable reached state set, where states
 * and precision are transformed by mapping functions.
 */
public class UnmodifiableReachedSetView
    implements UnmodifiableReachedSet {
  private final UnmodifiableReachedSet underlying;
  private final Function<? super AbstractState, AbstractState> mapStateFunction;
  private final Function<? super Precision, Precision> mapPrecisionFunction;

  public UnmodifiableReachedSetView(
      UnmodifiableReachedSet pUnderlyingSet,
      Function<? super AbstractState, AbstractState> pMapStateFunction,
      Function<? super Precision, Precision> pMapPrecisionFunction) {
    assert pUnderlyingSet != null;
    assert pMapStateFunction != null;
    assert pMapPrecisionFunction != null;

    underlying = pUnderlyingSet;
    mapStateFunction = pMapStateFunction;
    mapPrecisionFunction = pMapPrecisionFunction;
  }

  @Override
  public AbstractState getFirstState() {
    return mapStateFunction.apply(underlying.getFirstState());
  }

  @Override
  public AbstractState getLastState() {
    return mapStateFunction.apply(underlying.getLastState());
  }

  @Override
  public Precision getPrecision(AbstractState pState) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Unwrapping prevents reverse mapping");
  }

  @Override
  public Collection<AbstractState> asCollection() {
    return Collections2.transform(underlying.asCollection(), mapStateFunction);
  }

  @Override
  public Collection<AbstractState> getReached(AbstractState pState)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Unwrapping prevents knowing the partition");
  }

  @Override
  public Collection<AbstractState> getReached(CFANode pLocation) {
    return Collections2.transform(underlying.getReached(pLocation), mapStateFunction);
  }

  @Override
  public Collection<Precision> getPrecisions() {
    return Collections2.transform(underlying.getPrecisions(), mapPrecisionFunction);
  }

  @Override
  public Collection<AbstractState> getWaitlist() {
    return Collections2.transform(underlying.getWaitlist(), mapStateFunction);
  }

  @Override
  public boolean hasWaitingState() {
    return underlying.hasWaitingState();
  }

  @Override
  public Iterator<AbstractState> iterator() {
    return Iterators.transform(underlying.iterator(), mapStateFunction);
  }

  @Override
  public boolean contains(AbstractState pState) {
    throw new UnsupportedOperationException("Unwrapping may prevent to check contains");
  }

  @Override
  public boolean isEmpty() {
    return underlying.isEmpty();
  }

  @Override
  public int size() {
    return underlying.size();
  }

}
