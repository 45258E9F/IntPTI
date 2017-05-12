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

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

/**
 * Representation of a "return" statement,
 * potentially including a return value.
 */
public interface AReturnStatement extends AAstNode {

  /**
   * The return value, if present
   * (i.e., the "exp" in "return exp;").
   */
  public Optional<? extends AExpression> getReturnValue();

  /**
   * If this statement has a return value,
   * this method creates a representation of this statement in form of an assignment
   * of the return value to a special variable
   * (i.e., something like "__retval__ = exp;").
   * This special variable is the same as the one returned by
   * {@link FunctionEntryNode#getReturnVariable()}.
   */
  public Optional<? extends AAssignment> asAssignment();
}
