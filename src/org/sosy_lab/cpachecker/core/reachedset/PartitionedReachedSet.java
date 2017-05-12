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

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Special implementation of the reached set that partitions the set by keys that
 * depend on the abstract state.
 * Which key is used for an abstract state can be changed by overriding
 * {@link #getPartitionKey(AbstractState)} in a sub-class.
 * By default, this implementation needs abstract states which implement
 * {@link Partitionable} and uses the return value of {@link Partitionable#getPartitionKey()}
 * as the key.
 *
 * Whenever the method {@link PartitionedReachedSet#getReached(AbstractState)}
 * is called (which is usually done by the CPAAlgorithm to get the candidates
 * for merging and coverage checks), it will return a subset of the set of all
 * reached states. This subset contains exactly those states, whose partition
 * key is equal to the key of the state given as a parameter.
 */
public class PartitionedReachedSet extends DefaultReachedSet {

  private final Multimap<Object, AbstractState> partitionedReached =
      LinkedHashMultimap.create(100, 1);

  public PartitionedReachedSet(WaitlistFactory waitlistFactory) {
    super(waitlistFactory);
  }

  @Override
  public void add(AbstractState pState, Precision pPrecision) {
    super.add(pState, pPrecision);

    partitionedReached.put(getPartitionKey(pState), pState);
  }

  @Override
  public void remove(AbstractState pState) {
    super.remove(pState);

    partitionedReached.remove(getPartitionKey(pState), pState);
  }

  @Override
  public void clear() {
    super.clear();

    partitionedReached.clear();
  }

  @Override
  public Collection<AbstractState> getReached(AbstractState pState) {
    return getReachedForKey(getPartitionKey(pState));
  }

  public int getNumberOfPartitions() {
    return partitionedReached.keySet().size();
  }

  public Map.Entry<Object, Collection<AbstractState>> getMaxPartition() {
    int max = 0;
    Map.Entry<Object, Collection<AbstractState>> maxPartition = null;

    for (Map.Entry<Object, Collection<AbstractState>> partition : partitionedReached.asMap()
        .entrySet()) {
      int size = partition.getValue().size();
      if (size > max) {
        max = partition.getValue().size();
        maxPartition = partition;
      }
    }
    return maxPartition;
  }

  protected Object getPartitionKey(AbstractState pState) {
    assert pState instanceof Partitionable
        : "Partitionable states necessary for PartitionedReachedSet";
    return ((Partitionable) pState).getPartitionKey();
  }

  protected Collection<AbstractState> getReachedForKey(Object key) {
    return Collections.unmodifiableCollection(partitionedReached.get(key));
  }

  protected Set<?> getKeySet() {
    return Collections.unmodifiableSet(partitionedReached.keySet());
  }
}
