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

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.Collection;

/**
 * Standard stop operator, which always return false
 */
public class StopNeverOperator implements StopOperator {

  @Override
  public boolean stop(AbstractState el, Collection<AbstractState> reached, Precision precision)
      throws CPAException {
    return false;
  }

  private static final StopOperator instance = new StopNeverOperator();

  public static StopOperator getInstance() {
    return instance;
  }

}
