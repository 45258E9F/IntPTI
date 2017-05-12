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

import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;

/**
 * Interface for all possible right-hand sides of an assignment.
 */
public interface JRightHandSide extends ARightHandSide, JAstNode {

  public <R, X extends Exception> R accept(JRightHandSideVisitor<R, X> pV) throws X;

}
