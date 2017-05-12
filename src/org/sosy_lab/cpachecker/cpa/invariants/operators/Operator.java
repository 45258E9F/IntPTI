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
package org.sosy_lab.cpachecker.cpa.invariants.operators;

/**
 * Instances of implementations of this interface are operators that
 * can be applied to an operand of type O1 and an other operand of type
 * O2, producing a value of type R representing the result of the
 * operation.
 *
 * @param <O1> the type of the first operand.
 * @param <O2> the type of the second operand.
 * @param <R>  the type of the result.
 */
public interface Operator<O1, O2, R> {

  /**
   * Applies this operator to the given operands.
   *
   * @param operand1 the first operand to apply the operator to.
   * @param operand2 the second operand to apply the operator to.
   * @return the value resulting from applying the first operand to the second operand.
   */
  R apply(O1 operand1, O2 operand2);

}
