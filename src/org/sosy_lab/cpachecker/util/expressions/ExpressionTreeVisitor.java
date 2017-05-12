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
package org.sosy_lab.cpachecker.util.expressions;


public interface ExpressionTreeVisitor<LeafType, T, E extends Throwable> {

  T visit(And<LeafType> pAnd) throws E;

  T visit(Or<LeafType> pOr) throws E;

  T visit(LeafExpression<LeafType> pLeafExpression) throws E;

  T visitTrue() throws E;

  T visitFalse() throws E;

}
