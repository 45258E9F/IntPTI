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
package org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * Instances of implementing classes are CFA edges representing the
 * assignment of values to the program counter variable.
 */
public interface ProgramCounterValueAssignmentEdge extends CFAEdge {

  /**
   * Gets the assigned program counter value.
   *
   * @return the assigned program counter value.
   */
  public int getProgramCounterValue();

}