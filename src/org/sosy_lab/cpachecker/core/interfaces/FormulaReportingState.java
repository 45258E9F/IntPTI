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
package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

/**
 * Interface to implement in order for an abstract state
 * to be able to be over-approximated by a formula representing
 * the abstract state.
 */
public interface FormulaReportingState extends AbstractState {

  /**
   * Returns a non-instantiated formula over-approximating the state.
   */
  BooleanFormula getFormulaApproximation(FormulaManagerView manager, PathFormulaManager pfmgr);

}
