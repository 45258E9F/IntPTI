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
package org.sosy_lab.cpachecker.cpa.constraints.constraint;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;

/**
 * A {@link Constraint} with two operands, like 'equals'.
 */
public interface BinaryConstraint extends Constraint {

  SymbolicExpression getOperand1();

  SymbolicExpression getOperand2();

  Type getCalculationType();
}
