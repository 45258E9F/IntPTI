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

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

enum AlwaysTopDomain implements AbstractDomain {

  INSTANCE;

  @Override
  public boolean isLessOrEqual(AbstractState pElement1, AbstractState pElement2) {

    assert pElement1 == AlwaysTopState.INSTANCE;
    assert pElement2 == AlwaysTopState.INSTANCE;
    return true;
  }

  @Override
  public AbstractState join(AbstractState pElement1, AbstractState pElement2) {

    assert pElement1 == AlwaysTopState.INSTANCE;
    assert pElement2 == AlwaysTopState.INSTANCE;
    return AlwaysTopState.INSTANCE;
  }
}
