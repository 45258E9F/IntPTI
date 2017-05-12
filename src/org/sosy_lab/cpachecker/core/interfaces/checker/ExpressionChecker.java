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
package org.sosy_lab.cpachecker.core.interfaces.checker;

import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import javax.annotation.Nullable;

/**
 * Check if current operation has bug given the initial abstract state
 * Expression checkers require visiting the structure of expression. Multiple expression checkers
 * should share one expression visiting process.
 *
 * @param <T> type of abstract state
 * @param <S> type of element in abstract domain
 */
public interface ExpressionChecker<T extends AbstractState, S> extends GeneralChecker {

  /*
  One main design principle:
      Transfer relation computation should NOT be included in checker.
      We just check the possible error state and refine the state if possible.
  In check-and-refine process of transfer relation, transfer computation could happen.
   */

  /**
   * A core method that checks (and refines) current state
   *
   * @param rightHand the expression to be examined
   * @param cell      input expression cell
   * @return the resultant expression cell
   */
  ExpressionCell<T, S> checkAndRefine(
      CRightHandSide rightHand, ExpressionCell<T, S> cell,
      @Nullable CFAEdge cfaEdge)
      throws CPATransferException;


  /**
   * A core method that handles assignment.
   * For example, given an assignment x = y, the preState should be applied on y while the
   * postState should be applied on x since x gets changed after operation.
   *
   * @param assignment the assignment to be examined
   * @param cell       input expression cell
   * @return the resultant expression cell
   */
  AssignmentCell<T, S> checkAndRefine(
      CAssignment assignment, AssignmentCell<T, S> cell,
      @Nullable CFAEdge cfaEdge)
      throws CPATransferException;

  Class<S> getAbstractDomainClass();

}
