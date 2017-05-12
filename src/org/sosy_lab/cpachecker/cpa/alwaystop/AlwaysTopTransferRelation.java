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
package org.sosy_lab.cpachecker.cpa.alwaystop;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

enum AlwaysTopTransferRelation implements TransferRelation {

  INSTANCE;

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pElement, List<AbstractState> otherStates, Precision pPrecision) {

    assert pElement == AlwaysTopState.INSTANCE;
    assert pPrecision == AlwaysTopPrecision.INSTANCE;

    return Collections.singleton(AlwaysTopState.INSTANCE);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, List<AbstractState> otherStates, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    return getAbstractSuccessors(pState, otherStates, pPrecision);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pElement,
      List<AbstractState> pOtherElements, CFAEdge pCfaEdge, Precision pPrecision) {

    assert pElement == AlwaysTopState.INSTANCE;
    assert pPrecision == AlwaysTopPrecision.INSTANCE;

    return null;
  }

}
