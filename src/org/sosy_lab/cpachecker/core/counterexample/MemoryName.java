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
package org.sosy_lab.cpachecker.core.counterexample;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;

/**
 * Implementations of this interface provide the concrete state
 * with the name of the allocated memory, which stores the value for
 * the given address and expression.
 */
public interface MemoryName {

  /**
   * Returns the allocated memory name that stores the value
   * of the given {@link CExpression} exp with the given {@link Address} address.
   *
   * @param exp     The value of this expression is requested.
   * @param address The requested value is expected to be at this address.
   * @return The name of the memory that holds the value for the given expression at the given
   * address.
   */
  public String getMemoryName(CRightHandSide exp, Address address);

}