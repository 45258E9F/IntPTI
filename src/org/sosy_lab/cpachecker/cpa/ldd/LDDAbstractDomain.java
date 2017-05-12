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
package org.sosy_lab.cpachecker.cpa.ldd;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.ldd.LDDRegionManager;


public class LDDAbstractDomain implements AbstractDomain {

  private final LDDRegionManager regionManager;

  private final AbstractState topState;

  public LDDAbstractDomain(LDDRegionManager regionManager) {
    this.regionManager = regionManager;
    this.topState = new LDDAbstractState(regionManager.makeTrue());
  }

  @Override
  public boolean isLessOrEqual(AbstractState newElement, AbstractState reachedState)
      throws CPAException {
    if (this.topState.equals(reachedState) || newElement.equals(reachedState)) {
      return true;
    }
    if (newElement instanceof LDDAbstractState && reachedState instanceof LDDAbstractState) {
      LDDAbstractState lddElement1 = (LDDAbstractState) newElement;
      LDDAbstractState lddElement2 = (LDDAbstractState) reachedState;
      return this.regionManager.entails(lddElement1.getRegion(), lddElement2.getRegion());
    }
    return false;
  }

  @Override
  public AbstractState join(AbstractState pElement1, AbstractState pElement2) throws CPAException {
    if (isLessOrEqual(pElement1, pElement2)) {
      return pElement2;
    }
    if (isLessOrEqual(pElement2, pElement1)) {
      return pElement1;
    }
    if (pElement1 instanceof LDDAbstractState && pElement2 instanceof LDDAbstractState) {
      LDDAbstractState lddElement1 = (LDDAbstractState) pElement1;
      LDDAbstractState lddElement2 = (LDDAbstractState) pElement2;
      return new LDDAbstractState(
          this.regionManager.makeOr(lddElement1.getRegion(), lddElement2.getRegion()));
    }
    return this.topState;
  }

}
