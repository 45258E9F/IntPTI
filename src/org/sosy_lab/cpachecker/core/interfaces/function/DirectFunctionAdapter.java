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
package org.sosy_lab.cpachecker.core.interfaces.function;

import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import java.util.List;

/**
 * An enhanced version of original {@link FunctionAdapter}, with support of relevant CFA edge.
 * "Direct" means the function adapter is not used for expression checks.
 */
public interface DirectFunctionAdapter<T extends AbstractState, S> {

  /**
   * Evaluate a function call expression given function call expression.
   *
   * @param pFunctionCallExpression Function call expression
   * @param currentState            current state of type T
   * @param currentOtherStates      current complete abstract state list
   * @param edge                    current CFA edge
   * @return the result of function call. Note that developer is responsible to maintain the
   * side-effect by changing currentState, or containing new state in return value
   */
  S evaluateFunctionCallExpression(
      CFunctionCallExpression pFunctionCallExpression,
      T currentState,
      List<AbstractState> currentOtherStates,
      CFAEdge edge);

  /**
   * Check if a function call has known semantics.
   */
  boolean isRegistered(CFunctionCallExpression pCFunctionCallExpression);

}
