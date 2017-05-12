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
import org.sosy_lab.solver.api.InterpolatingProverEnvironment;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.logging.Level;

public class TreeInterpolation<T> extends AbstractTreeInterpolation<T> {

  /**
   * This strategy is similar to "Tree Interpolation in Vampire*" from Blanc et al.
   * In comparison to the paper, we directly use the post-order-sorted
   * formula-list instead of the tree. This is easier to implement.
   */
  public TreeInterpolation(
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
    final Deque<Pair<BooleanFormula, Integer>> itpStack = new ArrayDeque<>();
    for (int positionOfA = 0; positionOfA < p.getFirst().size() - 1; positionOfA++) {
      itps.add(
          getTreeInterpolant(interpolator, itpStack, p.getFirst(), p.getSecond(), positionOfA));
    }
    logger.log(Level.ALL, "received interpolants of tree :", itps);
    return flattenTreeItps(formulasWithStatesAndGroupdIds, itps);
  }

  private BooleanFormula getTreeInterpolant(
      final InterpolationManager.Interpolator<T> interpolator,
      final Deque<Pair<BooleanFormula, Integer>> itpStack,
      final List<Triple<BooleanFormula, AbstractState, T>> formulas,
      final List<Integer> startOfSubTree,
      final int positionOfA)
      throws SolverException, InterruptedException {

    // use a new prover, because we use several distinct interpolation-queries
    try (final InterpolatingProverEnvironment<T> itpProver = interpolator.newEnvironment()) {
      final int currentSubtree = startOfSubTree.get(positionOfA);

      // build partition A
      final List<T> A = new ArrayList<>();
      while (!itpStack.isEmpty() && currentSubtree <= itpStack.peekLast().getSecond()) {
        A.add(itpProver.push(itpStack.pollLast().getFirst()));
      }
      A.add(itpProver.push(formulas.get(positionOfA).getFirst()));

      assert itpStack.isEmpty() == (currentSubtree == 0) :
          "empty stack is only allowed, if we are in the left-most branch" +
              startOfSubTree + "@" + positionOfA + "=" + currentSubtree + " vs " + itpStack.size();

      // build partition B
      for (Pair<BooleanFormula, Integer> externalChild : itpStack) {
        itpProver.push(externalChild.getFirst());
      }
      for (int i = positionOfA + 1; i < formulas.size(); i++) {
        itpProver.push(formulas.get(i).getFirst());
      }

      final boolean check = itpProver.isUnsat();
      assert check : "asserted formulas should be UNSAT";

      // get interpolant via Craig interpolation
      final BooleanFormula interpolant = itpProver.getInterpolant(A);

      // update the stack for further computation
      itpStack.addLast(Pair.of(interpolant, currentSubtree));
      return interpolant;
    }
  }
}
