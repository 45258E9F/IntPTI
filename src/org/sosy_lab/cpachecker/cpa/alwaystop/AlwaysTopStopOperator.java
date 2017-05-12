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
package org.sosy_lab.cpachecker.cpa.alwaystop;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;

import java.util.Collection;

enum AlwaysTopStopOperator implements StopOperator {

  INSTANCE;

  @Override
  public boolean stop(
      AbstractState pElement,
      Collection<AbstractState> pReached, Precision pPrecision) {

    assert pElement == AlwaysTopState.INSTANCE;
    assert pPrecision == AlwaysTopPrecision.INSTANCE;
    assert Iterables.all(pReached, Predicates.<AbstractState>equalTo(AlwaysTopState.INSTANCE));

    return !pReached.isEmpty();
  }
}
