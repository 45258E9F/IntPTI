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
package org.sosy_lab.cpachecker.cfa.ast.c;


public interface CStatementVisitor<R, X extends Exception> {

  R visit(CExpressionStatement pIastExpressionStatement) throws X;

  /**
   * The left-hand side of an assignment statement might be a
   * variable:         v = ...;
   * pointer:          *v = ...;
   * array element:    v[...] = ...;
   * field reference:  ...->v = ...;
   */
  R visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement) throws X;

  R visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement) throws X;

  R visit(CFunctionCallStatement pIastFunctionCallStatement) throws X;

}
