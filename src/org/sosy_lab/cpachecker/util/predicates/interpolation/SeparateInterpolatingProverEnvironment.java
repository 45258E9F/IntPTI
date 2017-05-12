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
package org.sosy_lab.cpachecker.util.predicates.interpolation;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.FormulaManager;
import org.sosy_lab.solver.api.InterpolatingProverEnvironmentWithAssumptions;
import org.sosy_lab.solver.api.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This is a class that allows to use a different SMT solver for interpolation
 * than for the rest.
 * Whenever it is used, it copies the formulas to the interpolation SMT solver
 * and back accordingly.
 */
public class SeparateInterpolatingProverEnvironment<T>
    implements InterpolatingProverEnvironmentWithAssumptions<T> {

  private final FormulaManager mainFmgr;
  private final FormulaManager itpFmgr;
  private final InterpolatingProverEnvironmentWithAssumptions<T> itpEnv;

  public SeparateInterpolatingProverEnvironment(
      FormulaManager pMainFmgr, FormulaManager pItpFmgr,
      InterpolatingProverEnvironmentWithAssumptions<T> pItpEnv) {
    mainFmgr = checkNotNull(pMainFmgr);
    itpFmgr = checkNotNull(pItpFmgr);
    itpEnv = checkNotNull(pItpEnv);
  }

  @Override
  public T push(BooleanFormula mainF) {
    BooleanFormula itpF = itpFmgr.parse(mainFmgr.dumpFormula(mainF).toString());
    return itpEnv.push(itpF);
  }

  @Override
  public void pop() {
    itpEnv.pop();
  }

  @Override
  public T addConstraint(BooleanFormula constraint) {
    return itpEnv.addConstraint(convertToItp(constraint));
  }

  @Override
  public void push() {
    itpEnv.push();
  }

  @Override
  public boolean isUnsat() throws InterruptedException, SolverException {
    return itpEnv.isUnsat();
  }

  @Override
  public boolean isUnsatWithAssumptions(List<BooleanFormula> assumptions)
      throws SolverException, InterruptedException {
    return itpEnv.isUnsatWithAssumptions(assumptions);
  }

  @Override
  public void close() {
    itpEnv.close();
  }

  @Override
  public BooleanFormula getInterpolant(List<T> pFormulasOfA)
      throws SolverException, InterruptedException {
    BooleanFormula itpF = itpEnv.getInterpolant(pFormulasOfA);
    return convertToMain(itpF);
  }

  @Override
  public List<BooleanFormula> getSeqInterpolants(List<Set<T>> partitionedFormulas)
      throws SolverException, InterruptedException {
    final List<BooleanFormula> itps = itpEnv.getSeqInterpolants(partitionedFormulas);
    final List<BooleanFormula> result = new ArrayList<>();
    for (BooleanFormula itp : itps) {
      result.add(convertToMain(itp));
    }
    return result;
  }

  @Override
  public List<BooleanFormula> getTreeInterpolants(
      List<Set<T>> partitionedFormulas,
      int[] startOfSubTree)
      throws SolverException, InterruptedException {
    final List<BooleanFormula> itps =
        itpEnv.getTreeInterpolants(partitionedFormulas, startOfSubTree);
    final List<BooleanFormula> result = new ArrayList<>();
    for (BooleanFormula itp : itps) {
      result.add(convertToMain(itp));
    }
    return result;
  }

  private BooleanFormula convertToItp(BooleanFormula f) {
    return itpFmgr.parse(mainFmgr.dumpFormula(f).toString());
  }

  private BooleanFormula convertToMain(BooleanFormula f) {
    return mainFmgr.parse(itpFmgr.dumpFormula(f).toString());
  }

  @Override
  public Model getModel() throws SolverException {
    return itpEnv.getModel();
  }
}
