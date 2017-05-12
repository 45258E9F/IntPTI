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
package org.sosy_lab.cpachecker.util.predicates.smt;

import com.google.common.base.Optional;

import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.Model;
import org.sosy_lab.solver.api.OptimizationProverEnvironment;

/**
 * Wrapper for {@link OptimizationProverEnvironment} which unwraps the objective formula.
 */
public class OptimizationProverEnvironmentView implements OptimizationProverEnvironment {

  private final OptimizationProverEnvironment delegate;
  private final FormulaWrappingHandler wrappingHandler;

  OptimizationProverEnvironmentView(
      OptimizationProverEnvironment pDelegate,
      FormulaManagerView pFormulaManager
  ) {
    delegate = pDelegate;
    wrappingHandler = pFormulaManager.getFormulaWrappingHandler();
  }


  @Override
  public Void addConstraint(BooleanFormula constraint) {
    return delegate.addConstraint(constraint);
  }

  @Override
  public int maximize(Formula objective) {
    return delegate.maximize(wrappingHandler.unwrap(objective));
  }

  @Override
  public int minimize(Formula objective) {
    return delegate.minimize(wrappingHandler.unwrap(objective));
  }

  @Override
  public OptStatus check()
      throws InterruptedException, SolverException {
    return delegate.check();
  }

  @Override
  public void push() {
    delegate.push();
  }

  @Override
  public Void push(BooleanFormula f) {
    return delegate.push(f);
  }

  @Override
  public void pop() {
    delegate.pop();
  }

  @Override
  public boolean isUnsat() throws SolverException, InterruptedException {
    return delegate.isUnsat();
  }

  @Override
  public Optional<Rational> upper(int handle, Rational epsilon) {
    return delegate.upper(handle, epsilon);
  }

  @Override
  public Optional<Rational> lower(int handle, Rational epsilon) {
    return delegate.lower(handle, epsilon);
  }

  @Override
  public Model getModel() throws SolverException {
    return new ModelView(delegate.getModel(), wrappingHandler);
  }

  @Override
  public void close() {
    delegate.close();
  }
}
