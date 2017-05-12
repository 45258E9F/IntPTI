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
package org.sosy_lab.cpachecker.util;

import com.google.errorprone.annotations.ForOverride;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;

/**
 * Utility class to visit all wrapped abstract states
 * (including the wrapper states)
 */
public abstract class AbstractWrappedStateVisitor {

  /**
   * Operation to apply on an state when it is visited
   */
  @ForOverride
  protected abstract void process(AbstractState state);

  /**
   * Visit a given abstract state and all its sub-state
   */
  public final void visit(AbstractState state) {
    process(state);

    if (state instanceof AbstractWrapperState) {
      AbstractWrapperState wrapperState = (AbstractWrapperState) state;
      for (AbstractState wrappedState : wrapperState.getWrappedStates()) {
        visit(wrappedState);
      }
    }
  }

}
