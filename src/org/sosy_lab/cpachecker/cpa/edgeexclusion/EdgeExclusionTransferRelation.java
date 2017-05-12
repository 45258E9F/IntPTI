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
package org.sosy_lab.cpachecker.cpa.edgeexclusion;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This is the edge exclusion transfer relation. For excluded edges, it does
 * not produce any successors, for other edges it returns the singleton edge
 * exclusion (top) state.
 */
class EdgeExclusionTransferRelation extends SingleEdgeTransferRelation {

  static final TransferRelation INSTANCE = new EdgeExclusionTransferRelation();

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, List<AbstractState> otherStates, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    assert pState == EdgeExclusionState.TOP;
    EdgeExclusionPrecision precision = (EdgeExclusionPrecision) pPrecision;
    if (precision.isExcluded(pCfaEdge)) {
      return Collections.emptySet();
    }
    return Collections.singleton(EdgeExclusionState.TOP);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    return null;
  }

}
