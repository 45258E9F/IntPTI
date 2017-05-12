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
package org.sosy_lab.cpachecker.cpa.constraints;

import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.IdentifierAssignment;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.ProverEnvironment;


/**
 * Class for creating {@link Formula}s out of {@link Constraint}s
 */
public interface FormulaCreator {

  /**
   * Creates a {@link BooleanFormula} representing the given {@link Constraint}.
   *
   * @param pConstraint the constraint to create a formula of
   * @return a <code>Formula</code> representing the given constraint
   */
  BooleanFormula createFormula(Constraint pConstraint)
      throws UnrecognizedCCodeException, InterruptedException;

  /**
   * Creates a {@link BooleanFormula} representing the given {@link Constraint}.
   * Symbolic Identifiers in constraints are replaced by their known definite assignments, if
   * one exists.
   *
   * @param pConstraint         the constraint to create a formula of
   * @param pDefiniteAssignment the known definite assignments of symbolic identifiers
   * @return a <code>Formula</code> representing the given constraint
   */
  BooleanFormula createFormula(Constraint pConstraint, IdentifierAssignment pDefiniteAssignment)
      throws UnrecognizedCCodeException, InterruptedException;

  /**
   * Creates a {@link BooleanFormula} representing the given term-value assignment.
   *
   * <p>These assignments are usually returned by {@link ProverEnvironment#getModel()} after a
   * successful SAT check.</p>
   *
   * <p>Example: Given variable <code>a</code> and <code>5</code>, this method
   * returns the formula <code>a equals 5</code>
   * </p>
   *
   * @param pVariable       the variable as a formula to assign the given value to
   * @param pTermAssignment the value of the assignment
   * @return a <code>BooleanFormula</code> representing the given assignment
   */
  BooleanFormula transformAssignment(Formula pVariable, Object pTermAssignment);
}
