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
 * Dealing with formulas:
 * solvers interfaces, creating formulas from code, etc.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = {"SE_BAD_FIELD", "SE_TRANSIENT_FIELD_NOT_RESTORED"},
    justification = "serialization of formulas is currently unsupported")
package org.sosy_lab.cpachecker.util.predicates;