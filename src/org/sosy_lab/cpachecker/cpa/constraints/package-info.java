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
 * Contains classes for Constraints CPA.
 * Constraints CPA tracks constraints such as conditions in if- or while-statements.
 * The ConstraintsCPA is only useful in combination with a CPA creating symbolic values,
 * for example {@link org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA ValueAnalysisCPA} with
 * property <code>cpa.value.symbolic.useSymbolicValues</code> set to true.
 * Without symbolic execution, it's transfer relation will always return a state containing
 * no information.
 */
package org.sosy_lab.cpachecker.cpa.constraints;
