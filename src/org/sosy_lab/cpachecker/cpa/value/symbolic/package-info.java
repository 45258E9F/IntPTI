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
 * This package contains all {@link org.sosy_lab.cpachecker.cpa.value.type.Value Values}
 * of a symbolic nature and classes that they depend on, like factories.
 * All <code>Value</code> objects in this package usually are implementations of
 * {@link org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue SymbolicValue}
 * and are used in symbolic execution and in the
 * {@link org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA ConstraintsCPA}.
 */
package org.sosy_lab.cpachecker.cpa.value.symbolic;
