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
package org.sosy_lab.cpachecker.core.interfaces.conditions;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;

import java.util.List;


/**
 * Interface to implement in order for an object to be able to
 * contribute invariants to the invariant construction.
 */
public interface AssumptionReportingState {

  /**
   * Get the assumptions that the given abstract state
   * wants to report for its containing node's location.
   *
   * @return a (possibly empty) list of assumptions representing the assumptions to generate for the
   * given state
   */
  public List<CExpression> getAssumptions();

}
