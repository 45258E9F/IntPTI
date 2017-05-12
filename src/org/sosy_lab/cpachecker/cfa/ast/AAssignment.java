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
package org.sosy_lab.cpachecker.cfa.ast;


/**
 * Interface for all statements that contain an assignment.
 * Only sub-classes of {@link AStatement} may implement this interface.
 */
public interface AAssignment extends AAstNode {


  ALeftHandSide getLeftHandSide();

  ARightHandSide getRightHandSide();
}
