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

import com.google.common.collect.Maps;

import java.util.Map;


public abstract class CachingVisitor<LeafType, T, E extends Throwable>
    extends DefaultExpressionTreeVisitor<LeafType, T, E> {

  private final Map<ExpressionTree<LeafType>, T> memo = Maps.newHashMap();

  @Override
  protected T visitDefault(ExpressionTree<LeafType> pExpressionTree) throws E {
    T result = memo.get(pExpressionTree);
    if (result != null) {
      return result;
    }
    result = pExpressionTree.accept(new ExpressionTreeVisitor<LeafType, T, E>() {

      @Override
      public T visit(And<LeafType> pAnd) throws E {
        return cacheMissAnd(pAnd);
      }

      @Override
      public T visit(Or<LeafType> pOr) throws E {
        return cacheMissOr(pOr);
      }

      @Override
      public T visit(LeafExpression<LeafType> pLeafExpression) throws E {
        return cacheMissLeaf(pLeafExpression);
      }

      @Override
      public T visitTrue() throws E {
        return cacheMissTrue();
      }

      @Override
      public T visitFalse() throws E {
        return cacheMissFalse();
      }

    });
    memo.put(pExpressionTree, result);
    return result;
  }

  protected abstract T cacheMissAnd(And<LeafType> pAnd) throws E;

  protected abstract T cacheMissOr(Or<LeafType> pOr) throws E;

  protected abstract T cacheMissLeaf(LeafExpression<LeafType> pLeafExpression) throws E;

  protected abstract T cacheMissTrue() throws E;

  protected abstract T cacheMissFalse() throws E;

}
