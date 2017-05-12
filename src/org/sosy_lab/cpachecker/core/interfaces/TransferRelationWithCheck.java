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
package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReport;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;
import java.util.List;

/**
 * A transfer relation that supports check-and-refine operation
 */
public interface TransferRelationWithCheck extends TransferRelation {

  /**
   * Get all error reports in this transfer (and strengthen).
   * Corresponding to instant error reports.
   *
   * @return the collection of error reports
   */
  Collection<ErrorReport> getErrorReports();


  /**
   * Reset error reports in check manager.
   * Corresponding to instant error reports.
   * Since checker manager does not accumulate errors.
   */
  void resetErrorReports();


  /**
   * Invoke all checkers that support error dumping and collect all errors using the global
   * analysis results such as dead code checker.
   * Corresponding to delayed error reports.
   * WARNING: this method has side effects. You should call it only once.
   *
   * @return the collection of dumped errors.
   */
  Collection<ErrorReport> dumpErrorsAfterAnalysis();

  /**
   * Check defects in expression evaluation should be done BEFORE computing transfer relation and
   * strengthen since refinement effects the prior state only.
   *
   * @param preState      the state before transfer relation
   * @param preOtherState list of states of all domains before transfer relation
   * @param precision     precision for transfer relation
   * @param cfaEdge       an edge of the CFA
   * @return possibly refined prior states (unchanged when there's no refinement)
   */
  Collection<? extends AbstractState> checkAndRefineExpression(
      AbstractState preState,
      List<AbstractState> preOtherState,
      Precision precision,
      CFAEdge cfaEdge
  ) throws CPATransferException, InterruptedException;


  /**
   * Check defects in state transition should be done AFTER computing transfer relation and
   * strengthen.
   *
   * @param postState       the state after transfer relation
   * @param postOtherStates list of states of all domains after transfer relation
   * @param precision       precision for transfer relation
   * @param cfaEdge         an edge of the CFA
   * @return list of all abstract states which should replace the old one.
   */
  Collection<? extends AbstractState> checkAndRefineState(
      AbstractState postState,
      List<AbstractState> postOtherStates,
      Precision precision,
      CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException;

}
