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

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.util.Collection;
import java.util.Set;

/**
 * Advanced implementation of ReachedSet.
 * It groups states by location and allows fast access to all states with
 * the same location as a given one.
 */
public class LocationMappedReachedSet extends PartitionedReachedSet {

  public LocationMappedReachedSet(WaitlistFactory waitlistFactory) {
    super(waitlistFactory);
  }

  @Override
  public Collection<AbstractState> getReached(CFANode location) {
    return getReachedForKey(location);
  }

  @Override
  protected Object getPartitionKey(AbstractState pState) {
    CFANode location = AbstractStates.extractLocation(pState);
    assert location != null : "Location information necessary for LocationMappedReachedSet";
    return location;
  }

  @SuppressWarnings("unchecked")
  public Set<CFANode> getLocations() {
    // generic cast is safe because we only put CFANodes into it
    return (Set<CFANode>) super.getKeySet();
  }
}
