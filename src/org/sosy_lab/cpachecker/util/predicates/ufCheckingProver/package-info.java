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
 * This package contains an additional Prover-wrapper, which uses constraints to improve formulas
 * with uninterpreted functions.
 *
 * We use UFs to model some operations and let the solver choose arbitrary values
 * (over-approximation!) for the result of the UF. If a formula is UNSAT, we can ignore the UF.
 * Otherwise we try to compute a better result with the {@link org.sosy_lab.cpachecker.util.predicates.ufCheckingProver.FunctionApplicationManager}
 * and add an additional constraint for the UF. This iteratively improves the solver's model.
 *
 * The {@link org.sosy_lab.cpachecker.util.predicates.ufCheckingProver.FunctionApplicationManager}
 * depends on the program's analysis and matches the precise operations that are over-approximated
 * with UFs.
 */
package org.sosy_lab.cpachecker.util.predicates.ufCheckingProver;