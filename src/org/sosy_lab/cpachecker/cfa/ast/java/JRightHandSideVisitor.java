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

/**
 * Interface for the visitor pattern. Typically used with {@link JExpressionVisitor}
 * to evaluate expressions.
 *
 * @param <R> the return type of an evaluation.
 * @param <X> the exception thrown, if there are errors while evaluating an expression.
 */
public interface JRightHandSideVisitor<R, X extends Exception> extends JExpressionVisitor<R, X> {

  R visit(JMethodInvocationExpression pAFunctionCallExpression) throws X;

  R visit(JClassInstanceCreation pJClassInstanceCreation) throws X;

}
