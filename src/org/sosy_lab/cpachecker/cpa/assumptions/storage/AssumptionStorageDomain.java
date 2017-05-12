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
package org.sosy_lab.cpachecker.cpa.assumptions.storage;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

public class AssumptionStorageDomain implements AbstractDomain {

  private final FormulaManagerView formulaManager;
  private final BooleanFormulaManagerView bfmgr;

  public AssumptionStorageDomain(
      FormulaManagerView pFormulaManager) {
    formulaManager = pFormulaManager;
    bfmgr = formulaManager.getBooleanFormulaManager();
  }

  @Override
  public AbstractState join(AbstractState pElement1, AbstractState pElement2) {

    AssumptionStorageState storageElement1 = (AssumptionStorageState) pElement1;
    AssumptionStorageState storageElement2 = (AssumptionStorageState) pElement2;

    // create the disjunction of the stop formulas
    // however, if one of them is true, we would lose the information from the other
    // so handle these special cases separately
    BooleanFormula stopFormula1 = storageElement1.getStopFormula();
    BooleanFormula stopFormula2 = storageElement2.getStopFormula();
    BooleanFormula newStopFormula;
    if (bfmgr.isTrue(stopFormula1)) {
      newStopFormula = stopFormula2;
    } else if (bfmgr.isTrue(stopFormula2)) {
      newStopFormula = stopFormula1;
    } else {
      newStopFormula = bfmgr.or(stopFormula1, stopFormula2);
    }

    return new AssumptionStorageState(
        formulaManager,
        bfmgr.and(storageElement1.getAssumption(),
            storageElement2.getAssumption()),
        newStopFormula);
  }

  @Override
  public boolean isLessOrEqual(AbstractState pElement1, AbstractState pElement2) {
    throw new UnsupportedOperationException();
  }
}