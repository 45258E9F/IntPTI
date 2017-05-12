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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.GET_BLOCK_FORMULA;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefiner.transformPath;
import static org.sosy_lab.cpachecker.util.AbstractStates.toState;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.refinement.InfeasiblePrefix;
import org.sosy_lab.cpachecker.util.refinement.InfeasiblePrefix.RawInfeasiblePrefix;
import org.sosy_lab.cpachecker.util.refinement.PrefixProvider;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.InterpolatingProverEnvironmentWithAssumptions;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@Options(prefix = "cpa.predicate.refinement")
public class PredicateBasedPrefixProvider implements PrefixProvider {
  @Option(secure = true, description = "Max. number of prefixes to extract")
  private int maxPrefixCount = 64;

  @Option(secure = true, description = "Max. length of feasible prefixes to extract from if at least one prefix was already extracted")
  private int maxPrefixLength = 1024;

  private final LogManager logger;

  private final Solver solver;

  private final PathFormulaManager pathFormulaManager;

  /**
   * This method acts as the constructor of the class.
   *
   * @param pSolver the solver to use
   */
  public PredicateBasedPrefixProvider(
      Configuration config,
      LogManager pLogger,
      Solver pSolver,
      PathFormulaManager pPathFormulaManager) {
    try {
      config.inject(this);
    } catch (InvalidConfigurationException e) {
      pLogger.log(Level.INFO, "Invalid configuration given to " + getClass().getSimpleName()
          + ". Using defaults instead.");
    }

    logger = pLogger;
    solver = pSolver;
    pathFormulaManager = pPathFormulaManager;
  }

  @Override
  public List<InfeasiblePrefix> extractInfeasiblePrefixes(final ARGPath pPath)
      throws CPAException, InterruptedException {

    List<ARGState> abstractionStates = transformPath(pPath);
    List<BooleanFormula> blockFormulas = from(abstractionStates)
        .transform(AbstractStates.toState(PredicateAbstractState.class))
        .transform(GET_BLOCK_FORMULA)
        .toList();

    List<Object> terms = new ArrayList<>(abstractionStates.size());
    List<RawInfeasiblePrefix> rawPrefixes = new ArrayList<>();

    try (@SuppressWarnings("unchecked")
         InterpolatingProverEnvironmentWithAssumptions<Object> prover =
             (InterpolatingProverEnvironmentWithAssumptions<Object>) solver
                 .newProverEnvironmentWithInterpolation()) {

      List<BooleanFormula> pathFormula = new ArrayList<>();
      PathFormula formula = pathFormulaManager.makeEmptyPathFormula();

      int currentBlockIndex = 0;

      PathIterator iterator = pPath.pathIterator();
      while (iterator.hasNext()) {
        ARGState currentState = iterator.getAbstractState();

        if (iterator.getIndex() == 0) {
          assert (isAbstractionState(currentState));
        }

        // only compute prefixes at abstraction states
        if (isAbstractionState(currentState)) {

          BooleanFormula currentBlockFormula = blockFormulas.get(currentBlockIndex);
          pathFormula.add(currentBlockFormula);

          try {
            formula = pathFormulaManager.makeAnd(makeEmpty(formula), currentBlockFormula);
            Object term = prover.push(formula.getFormula());
            terms.add(term);

            if (checkUnsat(pPath, iterator.getOutgoingEdge()) && prover.isUnsat()) {

              logger.log(Level.FINE, "found infeasible prefix, ending with edge ",
                  iterator.getOutgoingEdge(),
                  " in block # ",
                  currentBlockIndex,
                  ", that resulted in an unsat-formula");

              List<BooleanFormula> interpolantSequence = extractInterpolantSequence(terms, prover);
              List<BooleanFormula> finalPathFormula = new ArrayList<>(pathFormula);

              // create and add infeasible prefix, mind that the ARGPath has not (!)
              // failing assume operations replaced with no-ops, as this is not needed here,
              // and it would be cumbersome for ABE, so lets skip it
              ARGPath currentPrefixPath = ARGUtils.getOnePathTo(currentState);

              // put prefix data into a simple container for now
              rawPrefixes.add(new RawInfeasiblePrefix(currentPrefixPath,
                  interpolantSequence,
                  finalPathFormula));

              // stop once threshold for max. length of prefix is reached, relevant
              // e.g., for ECA programs where error paths often exceed 10.000 transition
              if (currentPrefixPath.size() >= maxPrefixLength) {
                break;
              }

              // remove reason for UNSAT from solver stack
              prover.pop();

              // replace respective term by tautology
              terms.remove(terms.size() - 1);
              formula = pathFormulaManager.makeAnd(makeEmpty(formula), makeTrue());
              terms.add(prover.push(formula.getFormula()));

              // replace failing block formula by tautology, too
              pathFormula.remove(pathFormula.size() - 1);
              pathFormula.add(makeTrue());
            }
          } catch (SolverException e) {
            throw new CPAException("Error during computation of prefixes: " + e.getMessage(), e);
          }

          currentBlockIndex++;

          // put hard-limit on number of prefixes
          if (rawPrefixes.size() == maxPrefixCount) {
            break;
          }
        }

        iterator.advance();
      }
    }

    // finally, create actual prefixes after solver stack is empty again,
    // doing it that way avoids problems with SMTInterpol (cf. commit 20405)
    List<InfeasiblePrefix> infeasiblePrefixes = new ArrayList<>(rawPrefixes.size());
    for (RawInfeasiblePrefix rawPrefix : rawPrefixes) {
      infeasiblePrefixes
          .add(InfeasiblePrefix.buildForPredicateDomain(rawPrefix, solver.getFormulaManager()));
    }

    return infeasiblePrefixes;
  }

  private <T> List<BooleanFormula> extractInterpolantSequence(
      final List<T> pTerms,
      final InterpolatingProverEnvironmentWithAssumptions<T> pProver)
      throws SolverException, InterruptedException {

    List<BooleanFormula> interpolantSequence = new ArrayList<>();

    for (int i = 1; i < pTerms.size(); i++) {
      interpolantSequence.add(pProver.getInterpolant(pTerms.subList(0, i)));
    }

    return interpolantSequence;
  }

  /**
   * This method checks if a unsat call is necessary. It the path is not single-block encoded,
   * then unsatisfiability has to be check always. In case it is single-block encoded,
   * then it suffices to check unsatisfiability at assume edges.
   *
   * @param pPath    the path to check
   * @param pCfaEdge the current edge
   * @return true if a unsat call is neccessary, else false
   */
  private boolean checkUnsat(final ARGPath pPath, final CFAEdge pCfaEdge) {
    if (!isSingleBlockEncoded(pPath)) {
      return true;
    }

    return pCfaEdge.getEdgeType() == CFAEdgeType.AssumeEdge;
  }

  /**
   * This method checks if the given path is single-block-encoded, which is the case,
   * if all states in the path are abstraction states.
   *
   * @param pPath the path to check
   * @return true, if all states in the path are abstraction states, else false
   */
  private boolean isSingleBlockEncoded(final ARGPath pPath) {
    return from(pPath.asStatesList()).allMatch(isAbstractionState());
  }

  private Predicate<AbstractState> isAbstractionState() {
    return Predicates.compose(PredicateAbstractState.FILTER_ABSTRACTION_STATES,
        toState(PredicateAbstractState.class));
  }

  private boolean isAbstractionState(ARGState pCurrentState) {
    return AbstractStates.toState(PredicateAbstractState.class).apply(pCurrentState)
        .isAbstractionState();
  }

  private PathFormula makeEmpty(PathFormula formula) {
    return pathFormulaManager.makeEmptyPathFormula(formula);
  }

  private BooleanFormula makeTrue() {
    return solver.getFormulaManager().getBooleanFormulaManager().makeBoolean(true);
  }
}
