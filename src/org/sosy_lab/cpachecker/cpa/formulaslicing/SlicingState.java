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
package org.sosy_lab.cpachecker.cpa.formulaslicing;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public abstract class SlicingState implements AbstractState {

  /**
   * Cast to subclass. Syntax sugar.
   */
  public SlicingIntermediateState asIntermediate() {
    return (SlicingIntermediateState) this;
  }

  public SlicingAbstractedState asAbstracted() {
    return (SlicingAbstractedState) this;
  }

  public abstract boolean isAbstracted();
}
