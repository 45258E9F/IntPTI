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
package org.sosy_lab.cpachecker.cpa.arg.counterexamples;

import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;

/**
 * Dummy implementation of {@link CounterexampleFilter}
 * that does not filter any counterexamples.
 */
public class NullCounterexampleFilter implements CounterexampleFilter {

  public NullCounterexampleFilter() {
  }

  @Override
  public boolean isRelevant(CounterexampleInfo pCounterexample) {
    return true;
  }
}
