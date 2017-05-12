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
package org.sosy_lab.cpachecker.cpa.location;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.location.LocationState.LocationStateFactory;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class LocationTransferRelationBackwards implements TransferRelation {

  private final LocationStateFactory factory;

  public LocationTransferRelationBackwards(LocationStateFactory pFactory) {
    factory = pFactory;
  }

  @Override
  public Collection<LocationState> getAbstractSuccessorsForEdge(
      AbstractState state, List<AbstractState> otherStates, Precision prec, CFAEdge cfaEdge)
      throws CPATransferException {

    LocationState predState = (LocationState) state;
    CFANode predLocation = predState.getLocationNode();

    if (CFAUtils.allEnteringEdges(predLocation).contains(cfaEdge)) {
      return Collections.singleton(factory.getState(cfaEdge.getPredecessor()));
    }

    return Collections.emptySet();
  }

  @Override
  public Collection<LocationState> getAbstractSuccessors(
      AbstractState state,
      List<AbstractState> otherStates, Precision prec) throws CPATransferException {

    CFANode predLocation = ((LocationState) state).getLocationNode();

    List<LocationState> allSuccessors = new ArrayList<>(predLocation.getNumEnteringEdges());

    for (CFANode predecessor : CFAUtils.predecessorsOf(predLocation)) {
      allSuccessors.add(factory.getState(predecessor));
    }

    return allSuccessors;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState element,
      List<AbstractState> otherElements, CFAEdge cfaEdge, Precision precision) {
    return null;
  }
}
