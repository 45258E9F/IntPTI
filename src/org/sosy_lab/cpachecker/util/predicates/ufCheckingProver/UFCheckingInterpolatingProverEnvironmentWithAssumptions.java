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
package org.sosy_lab.cpachecker.util.predicates.ufCheckingProver;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.InterpolatingProverEnvironmentWithAssumptions;

import java.util.List;
import java.util.Set;


public class UFCheckingInterpolatingProverEnvironmentWithAssumptions<T>
    extends UFCheckingBasicProverEnvironment<T>
    implements InterpolatingProverEnvironmentWithAssumptions<T> {

  private final InterpolatingProverEnvironmentWithAssumptions<T> delegate;

  public UFCheckingInterpolatingProverEnvironmentWithAssumptions(
      LogManager pLogger, InterpolatingProverEnvironmentWithAssumptions<T> ipe,
      FormulaManagerView pFmgr, UFCheckingProverOptions options) {
    super(pLogger, ipe, pFmgr, options);
    this.delegate = ipe;
  }

  @Override
  public boolean isUnsatWithAssumptions(List<BooleanFormula> pAssumptions)
      throws SolverException, InterruptedException {
    // TODO forward to isUnsat() ??
    return delegate.isUnsatWithAssumptions(pAssumptions);
  }

  @Override
  public BooleanFormula getInterpolant(List<T> formulasOfA)
      throws SolverException, InterruptedException {
    return delegate.getInterpolant(formulasOfA);
  }

  @Override
  public List<BooleanFormula> getSeqInterpolants(List<Set<T>> partitionedFormulas)
      throws SolverException, InterruptedException {
    return delegate.getSeqInterpolants(partitionedFormulas);
  }

  @Override
  public List<BooleanFormula> getTreeInterpolants(
      List<Set<T>> partitionedFormulas,
      int[] startOfSubTree)
      throws SolverException, InterruptedException {
    return delegate.getTreeInterpolants(partitionedFormulas, startOfSubTree);
  }
}
