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

/**
 * This interface represents expressions, that can be part of {@link JRunTimeTypeEqualsType}.
 * These expression evaluate to the run time type of the expression if they are part of
 * {@link JRunTimeTypeEqualsType}.
 */
public interface JRunTimeTypeExpression extends JExpression {

  boolean isThisReference();

  boolean isVariableReference();

}
