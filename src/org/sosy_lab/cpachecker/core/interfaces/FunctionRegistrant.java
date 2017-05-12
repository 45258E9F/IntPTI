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

import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;

public interface FunctionRegistrant {

  /**
   * Retrieve if current call expression is supported by function adapter
   *
   * @param pCFunctionCallExpression a function call expression
   * @return whether its operational semantics is modelled
   */
  boolean retrieveCall(CFunctionCallExpression pCFunctionCallExpression);

}
