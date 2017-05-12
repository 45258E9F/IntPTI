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
package org.sosy_lab.cpachecker.core.algorithm.invariants;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

class FormulaAndTreeSupplier implements InvariantSupplier, ExpressionTreeSupplier {

  private final InvariantSupplier invariantSupplier;

  private final ExpressionTreeSupplier expressionTreeSupplier;

  public FormulaAndTreeSupplier(
      InvariantSupplier pInvariantSupplier,
      ExpressionTreeSupplier pExpressionTreeSupplier) {
    this.invariantSupplier = pInvariantSupplier;
    this.expressionTreeSupplier = pExpressionTreeSupplier;
  }

  @Override
  public ExpressionTree<Object> getInvariantFor(CFANode pNode) {
    return expressionTreeSupplier.getInvariantFor(pNode);
  }

  @Override
  public BooleanFormula getInvariantFor(
      CFANode pNode, FormulaManagerView pFmgr, PathFormulaManager pPfmgr, PathFormula pContext) {
    return invariantSupplier.getInvariantFor(pNode, pFmgr, pPfmgr, pContext);
  }

}