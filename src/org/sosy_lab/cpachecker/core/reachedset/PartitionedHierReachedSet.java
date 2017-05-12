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

public class PartitionedHierReachedSet extends HierarchicalReachedSet {

  /**
   * Partitioned reached set is only associated with working reached set.
   */
  private final Multimap<Object, AbstractState> partitionedReached = LinkedHashMultimap.create
      (100, 1);

  PartitionedHierReachedSet(WaitlistFactory pWaitlistFactory) {
    super(pWaitlistFactory);
  }

  /* ********* */
  /* overrides */
  /* ********* */

  @Override
  public void add(AbstractState state, Precision precision) throws IllegalArgumentException {
    super.add(state, precision);
    partitionedReached.put(getPartitionKey(state), state);
  }

  @Override
  public void remove(AbstractState state) {
    super.remove(state);
    partitionedReached.remove(getPartitionKey(state), state);
  }

  @Override
  public void clear() {
    super.clear();
    partitionedReached.clear();
  }

  @Override
  public Collection<AbstractState> getReached(AbstractState state)
      throws UnsupportedOperationException {
    return getReachedForKey(getPartitionKey(state));
  }

  /* ***************** */
  /* partition methods */
  /* ***************** */

  protected Object getPartitionKey(AbstractState pState) {
    assert pState instanceof Partitionable : "State required to be partitionable";
    return ((Partitionable) pState).getPartitionKey();
  }

  Collection<AbstractState> getReachedForKey(Object pKey) {
    return Collections.unmodifiableCollection(partitionedReached.get(pKey));
  }

}
