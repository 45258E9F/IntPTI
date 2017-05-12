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

/**
 * This packages provides the encoding of (possibly-aliased) C pointers into SMT formulae, including
 * conditional updates for maybe-aliased pointers.
 * <p/>
 * The packages assumes that pointers of different types are never aliased.
 * <p/>
 * The package uses the SMT theory of arrays to model the heap memory with SMT
 * arrays, hence we need a SMT solver capable of the SMT theory of arrays. In
 * current CPAchecker, the following bundled solvers can be used:
 * <ul>
 * <li><emph>SMTInterpol</emph>&mdash;the default solver (configuration option:
 * <code>solver.solver=SMTINTERPOL</code>)</li>
 * <li><emph>MathSAT5</emph>&mdash;available with configuration option:
 * <code>solver.solver=MATHSAT5</code></li>
 * <li><emph>Princess</emph>&mdash;available with configuration option:
 * <code>solver.solver=PRINCESS</code>)</li>
 * <li><emph>Z3</emph>&mdash;available with configuration option:
 * <code>solver.solver=Z3</code></li>
 * </ul>
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "SE_BAD_FIELD",
    justification = "serialization of formulas is currently unsupported")
package org.sosy_lab.cpachecker.util.predicates.pathformula.heaparray;
