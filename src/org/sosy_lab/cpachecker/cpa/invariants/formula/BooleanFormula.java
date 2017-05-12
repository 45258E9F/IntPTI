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


public interface BooleanFormula<ConstantType> {

  <ReturnType> ReturnType accept(BooleanFormulaVisitor<ConstantType, ReturnType> pVisitor);

  /**
   * Accepts the given parameterized formula visitor.
   *
   * @param pVisitor   the visitor to accept.
   * @param pParameter the parameter to be handed to the visitor for this visit.
   * @return the result computed by the visitor for this specific boolean formula.
   */
  <ReturnType, ParamType> ReturnType accept(
      ParameterizedBooleanFormulaVisitor<ConstantType, ParamType, ReturnType> pVisitor,
      ParamType pParameter);

}
