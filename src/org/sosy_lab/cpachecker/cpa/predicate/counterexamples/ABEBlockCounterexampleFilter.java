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
package org.sosy_lab.cpachecker.cpa.predicate.counterexamples;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.toState;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.counterexamples.AbstractSetBasedCounterexampleFilter;
import org.sosy_lab.cpachecker.cpa.arg.counterexamples.CounterexampleFilter;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * An implementation of {@link CounterexampleFilter} that bases path similarity
 * on the ABE blocks the path contains
 * (to be more precise, on the sequence of abstraction locations along the path).
 */
public class ABEBlockCounterexampleFilter
    extends AbstractSetBasedCounterexampleFilter<ImmutableList<CFANode>> {

  public ABEBlockCounterexampleFilter(
      Configuration pConfig,
      LogManager pLogger,
      ConfigurableProgramAnalysis pCpa) {
    super(pConfig, pLogger, pCpa);
  }

  @Override
  protected Optional<ImmutableList<CFANode>> getCounterexampleRepresentation(CounterexampleInfo counterexample) {
    return Optional.of(
        from(counterexample.getTargetPath().asStatesList())
            .filter(Predicates.compose(PredicateAbstractState.FILTER_ABSTRACTION_STATES,
                toState(PredicateAbstractState.class)))
            .transform(AbstractStates.EXTRACT_LOCATION)
            .toList()
    );
  }
}
