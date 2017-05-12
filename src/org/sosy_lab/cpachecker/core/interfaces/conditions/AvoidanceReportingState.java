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
package org.sosy_lab.cpachecker.core.interfaces.conditions;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

/**
 * Interface to implement in order for an abstract state to be able to
 * make the system generate an assumption to avoid re-considering
 * this node.
 */
public interface AvoidanceReportingState extends AbstractState {

  /**
   * Returns true if an invariant must be added so as to avoid
   * the given state in the future.
   */
  public boolean mustDumpAssumptionForAvoidance();

  /**
   * If {@link #mustDumpAssumptionForAvoidance()} returned true, this method
   * returns a formula that provides an explanation. This formula may not be TRUE.
   * If the state cannot provide such a formula, it SHOULD return FALSE.
   * If {@link #mustDumpAssumptionForAvoidance()} returned false, this method
   * SHOULD return TRUE.
   */
  public BooleanFormula getReasonFormula(FormulaManagerView mgr);

}
