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
package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;


public enum SingletonAbstractState implements AbstractState {

  INSTANCE;

  @Override
  public String toString() {
    return "empty state";
  }


  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }
}
