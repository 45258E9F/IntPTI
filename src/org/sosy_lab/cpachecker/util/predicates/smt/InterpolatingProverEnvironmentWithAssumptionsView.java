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
package org.sosy_lab.cpachecker.util.predicates.smt;

import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.InterpolatingProverEnvironmentWithAssumptions;
import org.sosy_lab.solver.api.Model;

import java.util.List;
import java.util.Set;

/**
 * Model wrapping for InterpolatingProverEnvironment
 */
public class InterpolatingProverEnvironmentWithAssumptionsView<E> implements
                                                                  InterpolatingProverEnvironmentWithAssumptions<E> {

  private final InterpolatingProverEnvironmentWithAssumptions<E> delegate;
  private final FormulaWrappingHandler wrappingHandler;

  public InterpolatingProverEnvironmentWithAssumptionsView(
      InterpolatingProverEnvironmentWithAssumptions<E> pDelegate,
      FormulaWrappingHandler pWrappingHandler) {
    delegate = pDelegate;
    wrappingHandler = pWrappingHandler;
  }

  @Override
  public E push(BooleanFormula f) {
    return delegate.push(f);
  }

  @Override
  public void pop() {
    delegate.pop();
  }

  @Override
  public E addConstraint(BooleanFormula constraint) {
    return delegate.addConstraint(constraint);
  }

  @Override
  public void push() {
    delegate.push();
  }

  @Override
  public boolean isUnsat() throws SolverException, InterruptedException {
    return delegate.isUnsat();
  }

  @Override
  public Model getModel() throws SolverException {
    return new ModelView(delegate.getModel(), wrappingHandler);
  }

  @Override
  public void close() {
    delegate.close();
  }

  @Override
  public BooleanFormula getInterpolant(List<E> formulasOfA)
      throws SolverException, InterruptedException {
    return delegate.getInterpolant(formulasOfA);
  }

  @Override
  public List<BooleanFormula> getSeqInterpolants(
      List<Set<E>> partitionedFormulas)
      throws SolverException, InterruptedException {
    return delegate.getSeqInterpolants(partitionedFormulas);
  }

  @Override
  public List<BooleanFormula> getTreeInterpolants(
      List<Set<E>> partitionedFormulas, int[] startOfSubTree)
      throws SolverException, InterruptedException {
    return delegate.getTreeInterpolants(partitionedFormulas, startOfSubTree);
  }

  @Override
  public boolean isUnsatWithAssumptions(List<BooleanFormula> assumptions)
      throws SolverException, InterruptedException {
    return delegate.isUnsatWithAssumptions(assumptions);
  }
}
