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

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

enum AlwaysTopMergeOperator implements MergeOperator {

  INSTANCE;

  @Override
  public AbstractState merge(
      AbstractState pElement1,
      AbstractState pElement2, Precision pPrecision) {

    assert pElement1 == AlwaysTopState.INSTANCE;
    assert pElement2 == AlwaysTopState.INSTANCE;
    assert pPrecision == AlwaysTopPrecision.INSTANCE;
    return AlwaysTopState.INSTANCE;
  }

}
