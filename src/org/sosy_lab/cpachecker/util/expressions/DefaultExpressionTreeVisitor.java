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
package org.sosy_lab.cpachecker.util.expressions;


public abstract class DefaultExpressionTreeVisitor<LeafType, T, E extends Throwable>
    implements ExpressionTreeVisitor<LeafType, T, E> {

  protected abstract T visitDefault(ExpressionTree<LeafType> pExpressionTree) throws E;

  @Override
  public T visit(And<LeafType> pAnd) throws E {
    return visitDefault(pAnd);
  }

  @Override
  public T visit(Or<LeafType> pOr) throws E {
    return visitDefault(pOr);
  }

  @Override
  public T visit(LeafExpression<LeafType> pLeafExpression) throws E {
    return visitDefault(pLeafExpression);
  }

  @Override
  public T visitTrue() throws E {
    return visitDefault(ExpressionTrees.<LeafType>getTrue());
  }

  @Override
  public T visitFalse() throws E {
    return visitDefault(ExpressionTrees.<LeafType>getFalse());
  }

}
