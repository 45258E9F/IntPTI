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
package org.sosy_lab.cpachecker.cpa.location;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
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

import javax.annotation.Nullable;

public class LocationTransferRelationSingleFunction implements TransferRelation {

  private final LocationStateFactory factory;

  public LocationTransferRelationSingleFunction(LocationStateFactory pFactory) {
    factory = pFactory;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState state, List<AbstractState> otherStates, Precision precision)
      throws CPATransferException, InterruptedException {
    CFANode loc = ((LocationState) state).getLocationNode();
    List<LocationState> successors = new ArrayList<>(loc.getNumLeavingEdges());
    for (CFAEdge leavingEdge : CFAUtils.allLeavingEdges(loc)) {
      if (leavingEdge instanceof CFunctionCallEdge) {
        FunctionSummaryEdge summaryEdge = loc.getLeavingSummaryEdge();
        if (summaryEdge != null) {
          successors.add(factory.getState(summaryEdge.getSuccessor()));
        }
      } else {
        successors.add(factory.getState(leavingEdge.getSuccessor()));
      }
    }
    return successors;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, List<AbstractState> otherStates, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    LocationState locationState = (LocationState) state;
    CFANode loc = locationState.getLocationNode();
    if (CFAUtils.allLeavingEdges(loc).contains(cfaEdge)) {
      // if the upcoming CFA edge traverses multiple functions, we just block the analysis
      if (cfaEdge instanceof CFunctionCallEdge) {
        FunctionSummaryEdge summaryEdge = loc.getLeavingSummaryEdge();
        return summaryEdge != null ? Collections.singleton(factory.getState(summaryEdge
            .getSuccessor())) : Collections.<AbstractState>emptySet();
      } else if (cfaEdge instanceof CFunctionSummaryStatementEdge) {
        // we do not walk along function summary edge, otherwise the state space would explode
        return Collections.emptySet();
      } else {
        return Collections.singleton(factory.getState(cfaEdge.getSuccessor()));
      }
    } else if (loc.getNumLeavingEdges() == 1 && loc.getLeavingEdge(0) instanceof MultiEdge) {
      MultiEdge multiEdge = (MultiEdge) loc.getLeavingEdge(0);
      if (multiEdge.getEdges().get(0).equals(cfaEdge)) {
        return Collections.singleton(factory.getState(cfaEdge.getSuccessor()));
      }
    }
    return Collections.emptySet();
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      List<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision) throws CPATransferException, InterruptedException {
    return null;
  }
}
