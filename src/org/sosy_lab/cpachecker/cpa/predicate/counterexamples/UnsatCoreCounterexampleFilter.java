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
import com.google.common.collect.ImmutableList;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.counterexamples.CounterexampleFilter;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.ProverEnvironment;
import org.sosy_lab.solver.api.SolverContext.ProverOptions;

import java.util.List;
import java.util.logging.Level;

/**
 * A {@link CounterexampleFilter} that defines counterexamples as similar,
 * if the unsat core of their "negated paths" is the same.
 * The "negated path" of a counterexample is defined as the prefix of the path
 * until before the last AssumeEdge, and then the negation of that last AssumeEdge.
 *
 * If the negated path is not infeasible, the counterexample is considered
 * relevant (because no interpolants can be computed).
 */
public class UnsatCoreCounterexampleFilter
    extends AbstractNegatedPathCounterexampleFilter<ImmutableList<BooleanFormula>> {

  private final LogManager logger;
  private final Solver solver;

  public UnsatCoreCounterexampleFilter(
      Configuration pConfig, LogManager pLogger,
      ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException {
    super(pConfig, pLogger, pCpa);
    logger = pLogger;

    PredicateCPA predicateCpa = CPAs.retrieveCPA(pCpa, PredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(
          UnsatCoreCounterexampleFilter.class.getSimpleName() + " needs a PredicateCPA");
    }

    solver = predicateCpa.getSolver();
  }

  @Override
  protected Optional<ImmutableList<BooleanFormula>> getCounterexampleRepresentation(List<BooleanFormula> formulas)
      throws InterruptedException {

    try (ProverEnvironment thmProver = solver
        .newProverEnvironment(ProverOptions.GENERATE_UNSAT_CORE)) {

      for (BooleanFormula f : formulas) {
        thmProver.push(f);
      }

      if (!thmProver.isUnsat()) {
        // Negated path is not infeasible, cannot produce unsat core.
        // No filtering possible.
        return Optional.absent();
      }

      return Optional.of(ImmutableList.copyOf(thmProver.getUnsatCore()));

    } catch (SolverException e) {
      logger.logUserException(Level.WARNING, e,
          "Solving failed on counterexample path, cannot filter this counterexample");
      return Optional.absent();
    }
  }
}