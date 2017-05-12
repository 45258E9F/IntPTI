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
package org.sosy_lab.cpachecker.cpa.ldd;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.predicates.ldd.LDDRegion;

public class LDDAbstractState implements AbstractState {

  private LDDRegion region;

  public LDDAbstractState(LDDRegion region) {
    this.region = region;
  }

  public LDDRegion getRegion() {
    return this.region;
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }
}
