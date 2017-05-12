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
 * Converting code to formulas, and representation of program paths as formulas.
 * This includes the handling of re-assigned variables,
 * which get converted into SSA form.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = {"SE_BAD_FIELD", "SE_TRANSIENT_FIELD_NOT_RESTORED"},
    justification = "serialization of formulas is currently unsupported")
package org.sosy_lab.cpachecker.util.predicates.pathformula;