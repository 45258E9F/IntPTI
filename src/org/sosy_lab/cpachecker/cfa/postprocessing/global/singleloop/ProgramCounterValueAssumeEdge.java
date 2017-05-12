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
 * Edges of this interface are CFA assume edges used in the single loop
 * transformation. They are artificial edges used to encode the control flow
 * through the single loop head into the correct subgraph based on program
 * counter values.
 */
public interface ProgramCounterValueAssumeEdge extends CFAEdge {

  /**
   * Gets the program counter value.
   *
   * @return the program counter value.
   */
  public int getProgramCounterValue();

  /**
   * Checks if the assumption is assumed to be true or false on this edge.
   *
   * @return {@code true} is the assumption on this edge is assumed to be true, {@code false}
   * otherwise.
   */
  public boolean getTruthAssumption();

}