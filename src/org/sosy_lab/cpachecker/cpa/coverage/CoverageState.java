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
package org.sosy_lab.cpachecker.cpa.coverage;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class CoverageState implements AbstractState {

  private static CoverageState instance = new CoverageState();

  private CoverageState() {
  }

  public static CoverageState getSingleton() {
    Preconditions.checkNotNull(instance);
    return instance;
  }

  @Override
  public boolean equals(Object pObj) {
    return this == pObj;
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

}
