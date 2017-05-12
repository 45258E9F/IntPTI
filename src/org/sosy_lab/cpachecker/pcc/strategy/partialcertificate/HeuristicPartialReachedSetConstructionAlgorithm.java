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
package org.sosy_lab.cpachecker.pcc.strategy.partialcertificate;

import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PartialReachedConstructionAlgorithm;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.util.ArrayList;


public class HeuristicPartialReachedSetConstructionAlgorithm
    implements PartialReachedConstructionAlgorithm {

  @Override
  public AbstractState[] computePartialReachedSet(final UnmodifiableReachedSet pReached) {
    ArrayList<AbstractState> result = new ArrayList<>();
    CFANode node;
    for (AbstractState state : pReached.asCollection()) {
      node = AbstractStates.extractLocation(state);
      if (node == null || node.getNumEnteringEdges() > 1
          || (node.getNumLeavingEdges() > 0
          && node.getLeavingEdge(0).getEdgeType() == CFAEdgeType.FunctionCallEdge)) {
        result.add(state);
      }
    }
    if (!result.contains(pReached.getFirstState())) {
      result.add(pReached.getFirstState());
    }
    AbstractState[] arrayRep = new AbstractState[result.size()];
    result.toArray(arrayRep);
    return arrayRep;
  }

}
