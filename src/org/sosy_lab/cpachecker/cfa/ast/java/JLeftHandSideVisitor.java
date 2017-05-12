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
package org.sosy_lab.cpachecker.cfa.ast.java;

/**
 * Interface for the visitor pattern. Typically used with {@link org.sosy_lab.cpachecker.cfa.ast.java.JExpressionVisitor}
 * to evaluate expressions.
 *
 * @param <R> the return type of an evaluation.
 * @param <X> the exception thrown, if there are errors while evaluating an expression.
 */
public interface JLeftHandSideVisitor<R, X extends Exception> {

  R visit(JArraySubscriptExpression pAArraySubscriptExpression) throws X;

  R visit(JIdExpression pJIdExpression) throws X;
}
