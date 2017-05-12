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
package org.sosy_lab.cpachecker.cfa.ast.java;


public interface JStatementVisitor<R, X extends Exception> {

  public R visit(JExpressionAssignmentStatement pAExpressionAssignmentStatement) throws X;

  public R visit(JExpressionStatement pAExpressionStatement) throws X;

  public R visit(JMethodInvocationAssignmentStatement pAFunctionCallAssignmentStatement) throws X;

  public R visit(JMethodInvocationStatement pAFunctionCallStatement) throws X;

}
