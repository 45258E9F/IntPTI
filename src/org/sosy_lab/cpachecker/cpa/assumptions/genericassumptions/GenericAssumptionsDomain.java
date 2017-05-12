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
package org.sosy_lab.cpachecker.cpa.assumptions.genericassumptions;

import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class GenericAssumptionsDomain extends FlatLatticeDomain {

  public GenericAssumptionsDomain(AbstractState top) {
    super(top);
  }

  @Override
  public AbstractState join(AbstractState el1, AbstractState el2) throws CPAException {
    throw new UnsupportedOperationException();
  }
}
