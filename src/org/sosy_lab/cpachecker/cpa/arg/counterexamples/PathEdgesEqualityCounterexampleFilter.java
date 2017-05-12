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
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;


/**
 * A {@link CounterexampleFilter} that defines paths as similar
 * if they contain the exact same set of {@link CFAEdge}s,
 * but the order of the edges, and how many times they are visited along the path,
 * are irrelevant.
 *
 * This filter subsumes {@link PathEqualityCounterexampleFilter},
 * so if you use this class, you do not need to (additionally) use
 * {@link PathEqualityCounterexampleFilter}.
 */
public class PathEdgesEqualityCounterexampleFilter
    extends AbstractSetBasedCounterexampleFilter<ImmutableSet<CFAEdge>> {

  public PathEdgesEqualityCounterexampleFilter(
      Configuration pConfig,
      LogManager pLogger,
      ConfigurableProgramAnalysis pCpa) {
    super(pConfig, pLogger, pCpa);
  }

  @Override
  protected Optional<ImmutableSet<CFAEdge>> getCounterexampleRepresentation(CounterexampleInfo counterexample) {
    return Optional.of(ImmutableSet.copyOf(counterexample.getTargetPath().getInnerEdges()));
  }
}
