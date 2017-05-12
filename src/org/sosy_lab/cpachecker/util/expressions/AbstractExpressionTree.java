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

import com.google.common.base.Function;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.counterexample.CExpressionToOrinalCodeVisitor;

abstract class AbstractExpressionTree<LeafType> implements ExpressionTree<LeafType> {

  @Override
  public String toString() {
    return accept(
        new ToCodeVisitor<>(
            new Function<LeafType, String>() {

              @Override
              public String apply(LeafType pLeafExpression) {
                if (pLeafExpression instanceof CExpression) {
                  return ((CExpression) pLeafExpression)
                      .accept(CExpressionToOrinalCodeVisitor.INSTANCE);
                }
                if (pLeafExpression == null) {
                  return "null";
                }
                return pLeafExpression.toString();
              }
            }));
  }

}
