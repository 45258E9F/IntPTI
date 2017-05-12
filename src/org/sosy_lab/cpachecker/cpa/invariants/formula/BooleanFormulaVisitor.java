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


public interface BooleanFormulaVisitor<ConstantType, ReturnType> {

  /**
   * Visits the boolean constant {@code false}.
   *
   * @return the result of the visit.
   */
  ReturnType visitFalse();

  /**
   * Visits the boolean constant {@code true}.
   *
   * @return the result of the visit.
   */
  ReturnType visitTrue();

  /**
   * Visits the given equation invariants formula.
   *
   * @param pEqual the equation invariants formula to visit.
   * @return the result of the visit.
   */
  ReturnType visit(Equal<ConstantType> pEqual);

  /**
   * Visits the given less-than inequation invariants formula.
   *
   * @param pLessThan the less-than inequation invariants formula to visit.
   * @return the result of the visit.
   */
  ReturnType visit(LessThan<ConstantType> pLessThan);

  /**
   * Visits the given logical conjunction invariants formula.
   *
   * @param pAnd the logical conjunction invariants formula to visit.
   * @return the result of the visit.
   */
  ReturnType visit(LogicalAnd<ConstantType> pAnd);

  /**
   * Visits the given logical negation invariants formula.
   *
   * @param pNot the logical negation invariants formula to visit.
   * @return the result of the visit.
   */
  ReturnType visit(LogicalNot<ConstantType> pNot);

}
