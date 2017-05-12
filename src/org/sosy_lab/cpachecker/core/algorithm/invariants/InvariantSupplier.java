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
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;


public interface InvariantSupplier {

  /**
   * Return an invariant that holds at a given node.
   * This method should be relatively cheap and do not block
   * (i.e., do not start an expensive invariant generation procedure).
   *
   * @param node     The CFANode.
   * @param fmgr     The formula manager which should be used for creating the invariant formula.
   * @param pfmgr    The {@link PathFormulaManager} which should be used for creating the invariant
   *                 formula.
   * @param pContext the context of the formula.
   * @return An invariant boolean formula without SSA indices.
   */
  BooleanFormula getInvariantFor(
      CFANode node, FormulaManagerView fmgr, PathFormulaManager pfmgr, PathFormula pContext);

  static enum TrivialInvariantSupplier implements InvariantSupplier {
    INSTANCE;

    @Override
    public BooleanFormula getInvariantFor(
        CFANode pNode, FormulaManagerView pFmgr, PathFormulaManager pfmgr, PathFormula pContext) {
      return pFmgr.getBooleanFormulaManager().makeBoolean(true);
    }
  }
}
