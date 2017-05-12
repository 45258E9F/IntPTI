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
package org.sosy_lab.cpachecker.core.algorithm.invariants;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;


public interface ExpressionTreeSupplier {

  /**
   * Return an invariant that holds at a given node.
   * This method should be relatively cheap and not block
   * (i.e., do not start an expensive invariant generation procedure).
   *
   * @param node The CFANode.
   * @return An invariant boolean expression over C expressions.
   */
  ExpressionTree<Object> getInvariantFor(CFANode node);

  static enum TrivialInvariantSupplier implements ExpressionTreeSupplier {
    INSTANCE;

    @Override
    public ExpressionTree<Object> getInvariantFor(CFANode pNode) {
      return ExpressionTrees.getTrue();
    }
  }
}
