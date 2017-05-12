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

import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;

import java.util.List;

/**
 * A {@link CounterexampleFilter} that is a conjunction of other filters,
 * i.e., each filter needs to agree that a counterexample is relevant.
 *
 * Note that because {@link CounterexampleFilter}s are usually stateful,
 * it is important to put the weakest filter first,
 * i.e., the one that defines most counterexamples as relevant.
 * So usually the chain would start with something like a cheap but weak
 * {@link PathEqualityCounterexampleFilter},
 * and contain more expensive (and stronger) filters at the end.
 */
public class ConjunctiveCounterexampleFilter implements CounterexampleFilter {

  private final List<CounterexampleFilter> filters;

  public ConjunctiveCounterexampleFilter(List<CounterexampleFilter> pFilters) {
    filters = ImmutableList.copyOf(pFilters);
  }

  @Override
  public boolean isRelevant(CounterexampleInfo counterexample) throws InterruptedException {
    for (CounterexampleFilter filter : filters) {
      if (!filter.isRelevant(counterexample)) {
        return false;
      }
    }
    return true;
  }
}
