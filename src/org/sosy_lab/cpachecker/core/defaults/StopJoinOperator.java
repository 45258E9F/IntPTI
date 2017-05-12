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

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.Collection;
import java.util.Iterator;

/**
 * Standard stop-join operator that first joins all states
 * of the reached set into a single state, and then checks the
 * partial order relation.
 */
public class StopJoinOperator implements StopOperator {

  private final AbstractDomain domain;

  public StopJoinOperator(AbstractDomain domain) {
    this.domain = domain;
  }

  @Override
  public boolean stop(
      AbstractState state, Collection<AbstractState> reached,
      Precision precision) throws CPAException, InterruptedException {
    Iterator<AbstractState> it = reached.iterator();
    AbstractState joinedState = it.next();
    while (it.hasNext()) {
      joinedState = domain.join(it.next(), joinedState);
    }

    return domain.isLessOrEqual(state, joinedState);
  }
}
