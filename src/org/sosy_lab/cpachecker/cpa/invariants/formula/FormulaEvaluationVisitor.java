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
package org.sosy_lab.cpachecker.cpa.invariants.formula;

import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Map;

/**
 * Instances of implementing classes are invariants formula visitors used to
 * evaluate the visited formulae to values of their constant types.
 *
 * @param <T> the type of the constants used in the visited formulae.
 */
public interface FormulaEvaluationVisitor<T> extends
                                             ParameterizedNumeralFormulaVisitor<T, Map<? extends MemoryLocation, ? extends NumeralFormula<T>>, T>,
                                             ParameterizedBooleanFormulaVisitor<T, Map<? extends MemoryLocation, ? extends NumeralFormula<T>>, BooleanConstant<T>> {

}
