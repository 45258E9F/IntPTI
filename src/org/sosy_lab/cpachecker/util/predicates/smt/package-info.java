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

/**
 * Extensions of the pure {@link org.sosy_lab.solver.api.FormulaManager}
 * interface and its related interfaces
 * that make it easier to use by client code.
 * This package can be used regardless of which SMT solver is the backend.
 *
 * The most important feature of this package is to replace an SMT theory
 * with another one, simulating the semantics of the replaced theory
 * with other theories.
 * This can be used to allow working with {@link org.sosy_lab.solver.api.BitvectorFormula}
 * even if the solver does not support the theory of bitvectors.
 * Bitvectors will then be approximated with rationals or integers.
 */
package org.sosy_lab.cpachecker.util.predicates.smt;
