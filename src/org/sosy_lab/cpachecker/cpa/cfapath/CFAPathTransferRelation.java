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
package org.sosy_lab.cpachecker.cpa.cfapath;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CFAPathTransferRelation extends SingleEdgeTransferRelation {

  private static final Set<CFAPathTopState> topStateSingleton = CFAPathTopState.getSingleton();

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pElement,
      List<AbstractState> otherStates,
      Precision pPrecision,
      CFAEdge pCfaEdge)
      throws CPATransferException {
    if (pElement.equals(CFAPathTopState.getInstance())) {
      return topStateSingleton;
    }

    if (!(pElement instanceof CFAPathStandardState)) {
      throw new IllegalArgumentException();
    }

    CFAPathStandardState lCurrentElement = (CFAPathStandardState) pElement;

    CFAPathStandardState lSuccessor = new CFAPathStandardState(lCurrentElement, pCfaEdge);

    return Collections.singleton(lSuccessor);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pElement, List<AbstractState> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {

    return null;
  }

}
