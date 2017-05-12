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
package org.sosy_lab.cpachecker.util.predicates.interpolation.strategy;

import com.google.common.primitives.Ints;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;

import java.util.List;

public class TreeInterpolationWithSolver<T> extends AbstractTreeInterpolation<T> {

  /**
   * This strategy uses a SMT solver that directly computes tree interpolants.
   * The layout of the tree is explained in {@link AbstractTreeInterpolation}.
   * Currently only SMTInterpol and Z3 support this.
   */
  public TreeInterpolationWithSolver(
      LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      FormulaManagerView pFmgr, BooleanFormulaManager pBfmgr) {
    super(pLogger, pShutdownNotifier, pFmgr, pBfmgr);
  }

  @Override
  public List<BooleanFormula> getInterpolants(
      final InterpolationManager.Interpolator<T> interpolator,
      final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds)
      throws InterruptedException, SolverException {
    final Pair<List<Triple<BooleanFormula, AbstractState, T>>, List<Integer>> p =
        buildTreeStructure(formulasWithStatesAndGroupdIds);
    final List<BooleanFormula> itps = interpolator.itpProver.getTreeInterpolants(
        wrapAllInSets(projectToThird(p.getFirst())), Ints.toArray(p.getSecond()));
    return flattenTreeItps(formulasWithStatesAndGroupdIds, itps);
  }

}
