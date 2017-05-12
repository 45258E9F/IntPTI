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


public interface ExpressionTreeFactory<LeafType> {

  ExpressionTree<LeafType> leaf(LeafType pLeafType);

  ExpressionTree<LeafType> leaf(LeafType pLeafType, boolean pAssumeTruth);

  ExpressionTree<LeafType> and(ExpressionTree<LeafType> pOp1, ExpressionTree<LeafType> pOp2);

  ExpressionTree<LeafType> and(Iterable<ExpressionTree<LeafType>> pOperands);

  ExpressionTree<LeafType> or(ExpressionTree<LeafType> pOp1, ExpressionTree<LeafType> pOp2);

  ExpressionTree<LeafType> or(Iterable<ExpressionTree<LeafType>> pOperands);

}
