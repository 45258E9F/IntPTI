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
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.util.Collection;

public class LocationMappedHierReachedSet extends PartitionedHierReachedSet {

  LocationMappedHierReachedSet(WaitlistFactory pWaitlistFactory) {
    super(pWaitlistFactory);
  }

  @Override
  public Collection<AbstractState> getReached(CFANode location) {
    return getReachedForKey(location);
  }

  @Override
  protected Object getPartitionKey(AbstractState pState) {
    CFANode location = AbstractStates.extractLocation(pState);
    assert location != null : "Location information neccessary for location mapped reached set";
    return location;
  }
}
