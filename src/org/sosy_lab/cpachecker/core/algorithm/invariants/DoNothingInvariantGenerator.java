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
package org.sosy_lab.cpachecker.core.algorithm.invariants;

import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * Trivial implementation of an invariant generator
 * that does nothing and always returns the invariant true.
 */
public class DoNothingInvariantGenerator extends AbstractInvariantGenerator {

  @Override
  public void start(CFANode pInitialLocation) {
  }

  @Override
  public void cancel() {
  }

  @Override
  public boolean isProgramSafe() {
    return false;
  }
}
