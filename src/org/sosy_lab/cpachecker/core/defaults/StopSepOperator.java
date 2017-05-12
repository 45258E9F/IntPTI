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

/**
 * Standard stop-sep operator
 */
public class StopSepOperator implements StopOperator {

  private final AbstractDomain domain;

  /**
   * Creates a stop-sep operator based on the given
   * partial order
   */
  public StopSepOperator(AbstractDomain d) {
    domain = d;
  }

  @Override
  public boolean stop(AbstractState el, Collection<AbstractState> reached, Precision precision)
      throws CPAException, InterruptedException {

    for (AbstractState reachedState : reached) {
      if (domain.isLessOrEqual(el, reachedState)) {
        return true;
      }
    }
    return false;
  }
}
