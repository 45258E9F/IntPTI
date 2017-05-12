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

import org.sosy_lab.cpachecker.cpa.invariants.BitVectorType;

/**
 * Instances of implementing classes represent invariants formulae.
 *
 * @param <ConstantType> the type of the constants used in the formulae.
 */
public interface NumeralFormula<ConstantType> extends BitVectorType {

  /**
   * Accepts the given invariants formula visitor.
   *
   * @param pVisitor the visitor to accept.
   * @return the result computed by the visitor for this specific invariants formula.
   */
  <ReturnType> ReturnType accept(NumeralFormulaVisitor<ConstantType, ReturnType> pVisitor);

  /**
   * Accepts the given parameterized formula visitor.
   *
   * @param pVisitor   the visitor to accept.
   * @param pParameter the parameter to be handed to the visitor for this visit.
   * @return the result computed by the visitor for this specific invariants formula.
   */
  <ReturnType, ParamType> ReturnType accept(
      ParameterizedNumeralFormulaVisitor<ConstantType, ParamType, ReturnType> pVisitor,
      ParamType pParameter);

}
