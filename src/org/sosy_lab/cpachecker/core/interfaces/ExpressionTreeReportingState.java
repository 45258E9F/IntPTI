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

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;

/**
 * Interface to implement in order for an abstract state
 * to be able to be over-approximated by an ExpressionTree representing
 * the abstract state.
 */
public interface ExpressionTreeReportingState extends AbstractState {

  /**
   * Returns an ExpressionTree over-approximating the state.
   *
   * @param pFunctionScope the function scope as a function entry node.
   * @param pLocation      the formula should at least try to approximate variables referenced by
   *                       entering edges.
   * @return an ExpressionTree over-approximating the state.
   */
  ExpressionTree<Object> getFormulaApproximation(
      FunctionEntryNode pFunctionScope, CFANode pLocation);

}
