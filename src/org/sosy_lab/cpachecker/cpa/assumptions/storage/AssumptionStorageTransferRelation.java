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
package org.sosy_lab.cpachecker.cpa.assumptions.storage;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AssumptionReportingState;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Transfer relation and strengthening for the DumpInvariant CPA
 */
public class AssumptionStorageTransferRelation extends SingleEdgeTransferRelation {

  private final CtoFormulaConverter converter;
  private final FormulaManagerView formulaManager;

  private final Collection<AbstractState> topStateSet;

  public AssumptionStorageTransferRelation(
      CtoFormulaConverter pConverter,
      FormulaManagerView pFormulaManager, AbstractState pTopState) {
    converter = pConverter;
    formulaManager = pFormulaManager;
    topStateSet = Collections.singleton(pTopState);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pElement,
      List<AbstractState> otherStates,
      Precision pPrecision,
      CFAEdge pCfaEdge) {
    AssumptionStorageState element = (AssumptionStorageState) pElement;

    // If we must stop, then let's stop by returning an empty set
    if (element.isStop()) {
      return Collections.emptySet();
    }

    return topStateSet;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState el,
      List<AbstractState> others,
      CFAEdge edge,
      Precision p) throws CPATransferException, InterruptedException {
    AssumptionStorageState asmptStorageElem = (AssumptionStorageState) el;
    BooleanFormulaManagerView bfmgr = formulaManager.getBooleanFormulaManager();
    assert bfmgr.isTrue(asmptStorageElem.getAssumption());
    assert bfmgr.isTrue(asmptStorageElem.getStopFormula());
    String function = (edge.getSuccessor() != null) ? edge.getSuccessor().getFunctionName() : null;

    BooleanFormula assumption = bfmgr.makeBoolean(true);
    BooleanFormula stopFormula =
        bfmgr.makeBoolean(false); // initialize with false because we create a disjunction

    // process stop flag
    boolean stop = false;

    for (AbstractState element : AbstractStates.asFlatIterable(others)) {
      if (element instanceof AssumptionReportingState) {
        List<CExpression> assumptions = ((AssumptionReportingState) element).getAssumptions();
        for (CExpression inv : assumptions) {
          BooleanFormula invFormula =
              converter.makePredicate(inv, edge, function, SSAMap.emptySSAMap().builder());
          assumption = bfmgr.and(assumption, formulaManager.uninstantiate(invFormula));
        }
      }

      if (element instanceof AvoidanceReportingState) {
        AvoidanceReportingState e = (AvoidanceReportingState) element;

        if (e.mustDumpAssumptionForAvoidance()) {
          stopFormula = bfmgr.or(stopFormula, e.getReasonFormula(formulaManager));
          stop = true;
        }
      }
    }
    Preconditions.checkState(!bfmgr.isTrue(stopFormula));

    if (!stop) {
      stopFormula = bfmgr.makeBoolean(true);
    }

    if (bfmgr.isTrue(assumption) && bfmgr.isTrue(stopFormula)) {
      return null; // nothing has changed

    } else {
      return Collections
          .singleton(new AssumptionStorageState(formulaManager, assumption, stopFormula));
    }
  }
}