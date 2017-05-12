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
package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.solver.SolverException;

import java.util.Collection;

public class PredicatePCCStopOperator implements StopOperator {

  private final PredicateAbstractionManager paMgr;
  private final PathFormulaManager pMgr;

  private final AbstractionFormula trueAbs;

  public PredicatePCCStopOperator(
      PathFormulaManager pPfmgr, PredicateAbstractionManager pPredAbsManager) {
    paMgr = pPredAbsManager;
    pMgr = pPfmgr;

    trueAbs = paMgr.makeTrueAbstractionFormula(null);
  }

  @Override
  public boolean stop(
      AbstractState pState,
      Collection<AbstractState> pReached,
      Precision pPrecision)
      throws CPAException, InterruptedException {

    PredicateAbstractState e1 = (PredicateAbstractState) pState;

    for (AbstractState reachedState : pReached) {
      PredicateAbstractState e2 = (PredicateAbstractState) reachedState;

      try {
        if (isCoveredBy(e1, e2)) {
          return true;
        }
      } catch (SolverException e) {
        throw new CPAException("Solver Failure", e);
      }

    }
    return false;

  }

  private boolean isCoveredBy(final PredicateAbstractState e1, final PredicateAbstractState e2)
      throws InterruptedException, SolverException {
    if (e1.isAbstractionState() && e2.isAbstractionState()) {
      return paMgr.checkCoverage(e1.getAbstractionFormula(), e2.getAbstractionFormula());

    } else if (e2.isAbstractionState()) {
      return paMgr.checkCoverage(e1.getAbstractionFormula(), e1.getPathFormula(),
          e2.getAbstractionFormula());

    } else if (e1.isAbstractionState()) {
      return false;

    } else {
      if (e1.getAbstractionFormula() == e2.getAbstractionFormula()) {
        PathFormula pF = e1.getPathFormula();
        return paMgr.unsat(trueAbs,
            new PathFormula(pMgr.buildImplicationTestAsUnsat(pF, e2.getPathFormula()),
                pF.getSsa(), pF.getPointerTargetSet(), pF.getLength()));
      }
      return false;
    }
  }
}
