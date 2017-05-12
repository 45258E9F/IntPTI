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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;


public interface CandidateInvariant {

  /**
   * Gets the uninstantiated invariant formula.
   *
   * @param pFMGR    the formula manager.
   * @param pPFMGR   the path formula manager.
   * @param pContext the path formula context.
   * @return the uninstantiated invariant formula.
   * @throws CPATransferException if a CPA transfer required to produce the assertion failed.
   * @throws InterruptedException if the formula creation was interrupted.
   */
  BooleanFormula getFormula(
      FormulaManagerView pFMGR, PathFormulaManager pPFMGR, PathFormula pContext)
      throws CPATransferException, InterruptedException;

  /**
   * Creates an assertion of the invariant over the given reached set, using
   * the given formula managers.
   *
   * @param pReachedSet   the reached set to assert the invariant over.
   * @param pFMGR         the formula manager.
   * @param pPFMGR        the path formula manager.
   * @param pDefaultIndex the default SSA index.
   * @return the assertion.
   * @throws CPATransferException if a CPA transfer required to produce the assertion failed.
   * @throws InterruptedException if the formula creation was interrupted.
   */
  BooleanFormula getAssertion(
      Iterable<AbstractState> pReachedSet,
      FormulaManagerView pFMGR,
      PathFormulaManager pPFMGR,
      int pDefaultIndex)
      throws CPATransferException, InterruptedException;

  /**
   * Assume that the invariant holds and remove states from the given reached
   * set that must therefore be unreachable.
   *
   * @param pReachedSet the reached set to remove unreachable states from.
   */
  void assumeTruth(ReachedSet pReachedSet);

  /**
   * Try to inject the invariant into an invariant generator in order to
   * improve its results.
   *
   * @param pInvariantGenerator the invariant generator to inject the invariant into.
   * @throws UnrecognizedCodeException if a problem occurred during the injection.
   */
  void attemptInjection(InvariantGenerator pInvariantGenerator) throws UnrecognizedCodeException;

}
