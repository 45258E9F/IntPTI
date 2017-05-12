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
 * Utilities for refinement and refinement selection.
 *
 * <p>Contains Generic* classes which can be used for composing a simple refinement based on
 * refinement for abstract variable assignments.
 * Most of these are only dependent on interfaces
 * {@link org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator StrongestPostOperator},
 * {@link org.sosy_lab.cpachecker.util.refinement.Interpolant Interpolant}
 * , {@link org.sosy_lab.cpachecker.util.refinement.InterpolantManager InterpolantManager} and
 * {@link org.sosy_lab.cpachecker.util.refinement.ForgetfulState ForgetfulState}.
 * By defining implementations for these four interfaces, one can define a complete refinement
 * using the Generic* classes.</p>
 */
package org.sosy_lab.cpachecker.util.refinement;