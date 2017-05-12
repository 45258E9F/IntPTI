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
package org.sosy_lab.cpachecker.cfa.ast;

import org.sosy_lab.cpachecker.cfa.types.Type;

/**
 * Interfaces for all possible right-hand sides of an assignment.
 */
public interface ARightHandSide extends AAstNode {


  /**
   * This method returns the type of the expression.
   * If the expression is evaluated, the result of the evaluation has this type.
   * <p>
   * In some cases the parser can not determine the correct type
   * (because of missing information),
   * then this method can return a ProblemType.
   */
  public Type getExpressionType();


}
