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
 * Encoding of (possibly-aliased) C pointers into formulas,
 * including conditional updates for maybe-aliased pointers.
 * This package assumes that pointers of different types are never aliased.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "SE_BAD_FIELD",
    justification = "serialization of formulas is currently unsupported")
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;