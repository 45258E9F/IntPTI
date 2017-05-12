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
package org.sosy_lab.cpachecker.cpa.arg.counterexamples;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

/**
 * A {@link CounterexampleFilter} that defines paths as similar
 * if their representation as a list of {@link CFAEdge}s is equal.
 */
public class PathEqualityCounterexampleFilter
    extends AbstractSetBasedCounterexampleFilter<ImmutableList<CFAEdge>> {

  public PathEqualityCounterexampleFilter(
      Configuration pConfig,
      LogManager pLogger,
      ConfigurableProgramAnalysis pCpa) {
    super(pConfig, pLogger, pCpa);
  }

  @Override
  protected Optional<ImmutableList<CFAEdge>> getCounterexampleRepresentation(CounterexampleInfo counterexample) {
    return Optional.of(ImmutableList.copyOf(counterexample.getTargetPath().getInnerEdges()));
  }
}
