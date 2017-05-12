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
package org.sosy_lab.cpachecker.cpa.argReplay;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ARGReplayTransferRelation extends SingleEdgeTransferRelation {

  public ARGReplayTransferRelation() {
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState,
      List<AbstractState> otherStates,
      Precision pPrecision,
      CFAEdge pCfaEdge) {

    Set<ARGState> baseStates = ((ARGReplayState) pState).getStates();
    Set<ARGState> successors = new HashSet<>();

    // collect all states reachable via the edge
    for (ARGState baseState : baseStates) {

      // direct children
      getChildren(pCfaEdge, baseState, successors);

      // children of covering state
      // if (baseState.isCovered()) {
      //  ARGState coveringState = baseState.getCoveringState();
      //  logger.log(Level.INFO, "jumping from", pState, "to covering state", coveringState, "because of edge", pCfaEdge);
      //  getChildren(pCfaEdge, coveringState, successors);
      // }
    }

    return Collections
        .singleton(new ARGReplayState(successors, ((ARGReplayState) pState).getCPA()));
  }

  private void getChildren(CFAEdge pCfaEdge, ARGState baseState, Set<ARGState> successors) {
    for (ARGState child : baseState.getChildren()) {
      // normally only one child has the correct edge
      if (pCfaEdge.equals(baseState.getEdgeToChild(child))) {
        // redirect edge from child to covering state, because the subgraph of the covering state is reachable from there
        if (child.isCovered()) {
          successors.add(child.getCoveringState());
        } else {
          successors.add(child);
        }
      }
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {

    return Collections.singleton(pState);
  }

}
