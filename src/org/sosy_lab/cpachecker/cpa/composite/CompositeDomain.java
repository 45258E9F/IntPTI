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
package org.sosy_lab.cpachecker.cpa.composite;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.List;

public class CompositeDomain implements AbstractDomain {

  private final ImmutableList<AbstractDomain> domains;

  public CompositeDomain(ImmutableList<AbstractDomain> domains) {
    this.domains = domains;
  }

  @Override
  public AbstractState join(
      AbstractState pElement1,
      AbstractState pElement2) throws CPAException {
    // a simple join is here not possible, because it would over-approximate,
    // but join needs to return the least upper bound
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isLessOrEqual(AbstractState pElement1, AbstractState pElement2)
      throws CPAException, InterruptedException {
    CompositeState comp1 = (CompositeState) pElement1;
    CompositeState comp2 = (CompositeState) pElement2;

    List<AbstractState> comp1Elements = comp1.getWrappedStates();
    List<AbstractState> comp2Elements = comp2.getWrappedStates();

    Preconditions.checkState(comp1Elements.size() == comp2Elements.size());
    Preconditions.checkState(comp1Elements.size() == domains.size());

    for (int idx = 0; idx < comp1Elements.size(); idx++) {
      AbstractDomain domain = domains.get(idx);
      if (!domain.isLessOrEqual(comp1Elements.get(idx), comp2Elements.get(idx))) {
        return false;
      }
    }

    return true;
  }
}
