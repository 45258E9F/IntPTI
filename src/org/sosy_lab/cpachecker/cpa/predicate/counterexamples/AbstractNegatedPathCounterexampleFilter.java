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
package org.sosy_lab.cpachecker.cpa.predicate.counterexamples;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.ForOverride;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.counterexamples.AbstractSetBasedCounterexampleFilter;
import org.sosy_lab.cpachecker.cpa.arg.counterexamples.CounterexampleFilter;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * An abstract {@link CounterexampleFilter} implementation
 * for filters that use an SMT solver to generate a representation
 * of the "negated path" of the counterexample.
 * The "negated path" of a counterexample is defined as the prefix of the path
 * until before the last AssumeEdge, and then the negation of that last AssumeEdge.
 */
abstract class AbstractNegatedPathCounterexampleFilter<T>
    extends AbstractSetBasedCounterexampleFilter<T> {

  private final LogManager logger;

  private final PathFormulaManager pfmgr;

  protected AbstractNegatedPathCounterexampleFilter(
      Configuration pConfig, LogManager pLogger,
      ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException {
    super(pConfig, pLogger, pCpa);

    PredicateCPA predicateCpa = CPAs.retrieveCPA(pCpa, PredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(
          InterpolantPredicatesCounterexampleFilter.class.getSimpleName()
              + " needs a PredicateCPA");
    }

    logger = pLogger;
    pfmgr = predicateCpa.getPathFormulaManager();
  }

  @Override
  protected Optional<T> getCounterexampleRepresentation(CounterexampleInfo counterexample)
      throws InterruptedException {
    List<CFAEdge> edges = counterexample.getTargetPath().getInnerEdges();

    int cutPoint = edges.size() - 1; // Position of last AssumeEdge in path
    for (CFAEdge edge : Lists.reverse(edges)) {
      if (edge instanceof AssumeEdge) {
        break;
      }
      cutPoint--;
    }
    if (cutPoint < 0) {
      // no AssumEdge in path, cannot use this filter
      return Optional.absent();
    }

    AssumeEdge lastAssumeEdge = (AssumeEdge) edges.get(cutPoint);
    List<CFAEdge> prefix = edges.subList(0, cutPoint);

    PathFormula pf = pfmgr.makeEmptyPathFormula();
    List<BooleanFormula> formulas = new ArrayList<>(prefix.size() + 1);

    try {
      for (CFAEdge edge : prefix) {
        pf = pfmgr.makeAnd(pf, edge);
        formulas.add(pf.getFormula());
        pf = pfmgr.makeEmptyPathFormula(pf);
      }
      pf = pfmgr.makeAnd(pf, CFAUtils.getComplimentaryAssumeEdge(lastAssumeEdge));
      formulas.add(pf.getFormula());

    } catch (CPATransferException e) {
      logger.logUserException(Level.WARNING, e, "Failed to filter counterexample");
      return Optional.absent();
    }

    return getCounterexampleRepresentation(formulas);
  }

  /**
   * This method needs to produce an immutable representation of each counterexample.
   * If this filter does not manage to produce a meaningful representation of the current path,
   * it may return {@link Optional#absent()}. In this case, the counterexample
   * is considered relevant.
   * This method is given as input a list of {@link BooleanFormula}s
   * that represent the "negated path" of this counterexample.
   *
   * @param negatedPath A list of formulas, guaranteed to be not null.
   * @return An immutable representation of the counterexample, needs to have proper implementations
   * of {@link Object#equals(Object)} and {@link Object#hashCode()}, or {@link Optional#absent()}.
   */
  @ForOverride
  protected abstract Optional<T> getCounterexampleRepresentation(List<BooleanFormula> negatedPath)
      throws InterruptedException;
}
