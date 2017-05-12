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
package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;

/**
 * Interface that represents the union of a method and a constructor
 * Invocation.
 */
public interface JMethodOrConstructorInvocation extends AFunctionCall, JStatement {

  //TODO Investigate interface and the classes it implements, seems wrong

  @Override
  public JMethodInvocationExpression getFunctionCallExpression();
}
