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

import static com.google.common.collect.FluentIterable.from;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.Set;

public class TargetLocationCandidateInvariant extends AbstractLocationFormulaInvariant {

  public TargetLocationCandidateInvariant(Set<CFANode> pLocations) {
    super(pLocations);
  }

  @Override
  public BooleanFormula getFormula(
      FormulaManagerView pFMGR, PathFormulaManager pPFMGR, PathFormula pContext)
      throws CPATransferException, InterruptedException {
    return pFMGR.getBooleanFormulaManager().makeBoolean(false);
  }

  @Override
  public BooleanFormula getAssertion(
      Iterable<AbstractState> pReachedSet,
      FormulaManagerView pFMGR,
      PathFormulaManager pPFMGR,
      int pDefaultIndex) {
    Iterable<AbstractState> targetStates = from(pReachedSet).filter(AbstractStates.IS_TARGET_STATE);
    return pFMGR.getBooleanFormulaManager().not(
        BMCHelper.createFormulaFor(targetStates, pFMGR.getBooleanFormulaManager()));
  }

  @Override
  public void assumeTruth(ReachedSet pReachedSet) {
    Iterable<AbstractState> targetStates =
        from(pReachedSet).filter(AbstractStates.IS_TARGET_STATE).toList();
    pReachedSet.removeAll(targetStates);
    for (ARGState s : from(targetStates).filter(ARGState.class)) {
      s.removeFromARG();
    }
  }

  @Override
  public void attemptInjection(InvariantGenerator pInvariantGenerator) {
    // Not implemented
  }

  @Override
  public String toString() {
    return "No target locations reachable from: " + getLocations();
  }

  @Override
  public int hashCode() {
    return getLocations().hashCode();
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof TargetLocationCandidateInvariant) {
      return getLocations().equals(((TargetLocationCandidateInvariant) pObj).getLocations());
    }
    return false;
  }

}
