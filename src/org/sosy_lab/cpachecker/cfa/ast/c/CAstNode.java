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
package org.sosy_lab.cpachecker.cfa.ast.c;


import org.sosy_lab.cpachecker.cfa.ast.AAstNode;


public interface CAstNode extends AAstNode {

  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> v) throws X;

}