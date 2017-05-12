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
package org.sosy_lab.cpachecker.util.assumptions;

import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.NumeralFormula;
import org.sosy_lab.solver.api.NumeralFormula.RationalFormula;
import org.sosy_lab.solver.api.NumeralFormulaManager;

/**
 * Enum listing several possible reasons for giving up analysis at a certain point.
 */
public enum PreventingHeuristic {
  PATHLENGTH("PL"),
  SUCCESSORCOMPTIME("SCT"),
  PATHCOMPTIME("PCT"),
  ASSUMEEDGESINPATH("AEIP"),
  ASSIGNMENTSINPATH("ASIP"),
  REPETITIONSINPATH("RIP"),
  MEMORYUSED("MU"),
  MEMORYOUT("MO"),
  TIMEOUT("TO"),
  LOOPITERATIONS("LI"),
  RECURSIONDEPTH("RD"),
  EDGECOUNT("EC");

  private final String predicateString;

  private PreventingHeuristic(String predicateStr) {
    predicateString = predicateStr;
  }

  /**
   * Returns a formula of this reason, which includes the
   * threshold value which was exceeded.
   */
  public BooleanFormula getFormula(FormulaManagerView fmgr, long thresholdValue) {
    NumeralFormulaManager<NumeralFormula, RationalFormula> nfmgr = fmgr.getRationalFormulaManager();
    final RationalFormula number = nfmgr.makeNumber(thresholdValue);
    final RationalFormula var = nfmgr.makeVariable(predicateString);
    // TODO better idea?
    return nfmgr.equal(var, number);
  }
}