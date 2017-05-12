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
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
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

public class LocationTransferRelation implements TransferRelation {

  private final LocationStateFactory factory;

  public LocationTransferRelation(LocationStateFactory pFactory) {
    factory = pFactory;
  }

  @Override
  public Collection<LocationState> getAbstractSuccessorsForEdge(
      AbstractState element, List<AbstractState> otherStates, Precision prec, CFAEdge cfaEdge) {

    LocationState inputElement = (LocationState) element;
    CFANode node = inputElement.getLocationNode();

    if (CFAUtils.allLeavingEdges(node).contains(cfaEdge)) {
      return Collections.singleton(factory.getState(cfaEdge.getSuccessor()));

    } else if (node.getNumLeavingEdges() == 1
        && node.getLeavingEdge(0) instanceof MultiEdge) {
      // maybe we are "entering" a MultiEdge via it's first component edge
      MultiEdge multiEdge = (MultiEdge) node.getLeavingEdge(0);
      if (multiEdge.getEdges().get(0).equals(cfaEdge)) {
        return Collections.singleton(factory.getState(cfaEdge.getSuccessor()));
      }
    }

    return Collections.emptySet();
  }

  @Override
  public Collection<LocationState> getAbstractSuccessors(
      AbstractState element,
      List<AbstractState> otherStates, Precision prec) throws CPATransferException {

    CFANode node = ((LocationState) element).getLocationNode();

    List<LocationState> allSuccessors = new ArrayList<>(node.getNumLeavingEdges());

    for (CFANode successor : CFAUtils.successorsOf(node)) {
      allSuccessors.add(factory.getState(successor));
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
