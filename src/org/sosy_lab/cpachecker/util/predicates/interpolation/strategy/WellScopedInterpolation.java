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

import java.util.ArrayList;
import java.util.List;

public class WellScopedInterpolation<T> extends AbstractTreeInterpolation<T> {

  /**
   * This strategy returns a sequence of interpolants by computing
   * each interpolant for i={0..n-1} for the partitions
   * A=[lastFunctionEntryIndex .. i] and B=[0 .. lastFunctionEntryIndex-1 , i+1 .. n].
   * The resulting interpolants are based on a tree-like scheme.
   */
  public WellScopedInterpolation(
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
    final List<BooleanFormula> itps = new ArrayList<>();
    for (int end_of_A = 0; end_of_A < p.getFirst().size() - 1; end_of_A++) {
      // last iteration is left out because B would be empty
      final int start_of_A = p.getSecond().get(end_of_A);
      itps.add(getInterpolantFromSublist(interpolator.itpProver, projectToThird(p.getFirst()),
          start_of_A, end_of_A));
    }
    return flattenTreeItps(formulasWithStatesAndGroupdIds, itps);
  }

}
