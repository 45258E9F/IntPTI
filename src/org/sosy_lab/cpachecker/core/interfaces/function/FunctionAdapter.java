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
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.checker.ExpressionCell;

import java.util.List;

/**
 * A function adaptor makes the evaluation of function call expression more precise. Some library
 * functions especially numerical functions have determined semantics and moreover, their
 * implementation details are omitted which makes inter-procedural analysis impossible.
 *
 * @param <S> the type of abstract domain
 * @param <T> the type of abstract state
 */
public interface FunctionAdapter<T extends AbstractState, S> {

  /**
   * Evaluate a function call expression given argument values.
   *
   * @param pFunctionCallExpression Function call expression
   * @param arguments               values of arguments, its length should be consistent with the
   *                                arity of function call
   * @param currentState            current state of component T
   * @param currentOtherStates      current complete abstract state list
   * @return an expression cell containing a possibly changed state and resultant value
   */
  ExpressionCell<T, S> evaluateFunctionCallExpression
  (
      CFunctionCallExpression pFunctionCallExpression,
      List<S> arguments,
      T currentState,
      List<AbstractState> currentOtherStates);

  /**
   * Refine the domain values given range restriction.
   *
   * @param pCFunctionCallExpression Function call expression
   * @param restriction              value constraint (restriction) on range
   * @param currentState             current abstract state
   * @param currentOtherStates       current complete abstract state list
   * @return a refined abstract state of component T
   */
  T refineFunctionCallExpression
  (
      CFunctionCallExpression pCFunctionCallExpression,
      S restriction,
      T currentState,
      List<AbstractState> currentOtherStates);

  /**
   * Check if a function call is known (i.e. registered)
   *
   * @param pCFunctionCallExpression a function call expression
   * @return the registration status
   */
  boolean isRegistered(CFunctionCallExpression pCFunctionCallExpression);

}
