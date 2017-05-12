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
package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This transfer relation always returns the state itself as its successor.
 * I.e, the relation contains for all abstract states x and edges e the tuples
 * (x,e,x).
 */
public enum IdentityTransferRelation implements TransferRelation {

  INSTANCE;

  @Override
  public Collection<AbstractState> getAbstractSuccessors(
      AbstractState pState, List<AbstractState> otherStates, Precision pPrecision) {
    return Collections.singleton(pState);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState,
      List<AbstractState> otherStates,
      Precision pPrecision,
      CFAEdge pCfaEdge) {
    return Collections.singleton(pState);
  }

  @Override
  public Collection<AbstractState> strengthen(
      AbstractState pState,
      List<AbstractState> pOtherStates, CFAEdge pCfaEdge, Precision pPrecision) {

    return null;
  }
}
