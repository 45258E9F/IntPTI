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

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.base.Predicates.or;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * A {@link CounterexampleFilter} that ignores the concrete edges of paths
 * and looks only at the function call trees.
 * If those are equal, the paths are considered similar.
 *
 * Note that in the following program, paths through both branches are similar
 * (call locations are ignored, only function names matter):
 * <code>
 * void f() { }
 * void main() {
 * if (...) { f(); } else { f(); }
 * }
 * </code>
 *
 * This filter is cheap and subsumes {@link PathEqualityCounterexampleFilter}.
 */
public class CallTreeCounterexampleFilter
    extends AbstractSetBasedCounterexampleFilter<ImmutableList<CFANode>> {

  public CallTreeCounterexampleFilter(
      Configuration pConfig,
      LogManager pLogger,
      ConfigurableProgramAnalysis pCpa) {
    super(pConfig, pLogger, pCpa);
  }

  @Override
  protected Optional<ImmutableList<CFANode>> getCounterexampleRepresentation(CounterexampleInfo counterexample) {
    return Optional.of(
        from(counterexample.getTargetPath().asStatesList())
            .transform(AbstractStates.EXTRACT_LOCATION)
            .filter(or(
                instanceOf(FunctionEntryNode.class),
                instanceOf(FunctionExitNode.class)))
            .toList()
    );
  }
}
