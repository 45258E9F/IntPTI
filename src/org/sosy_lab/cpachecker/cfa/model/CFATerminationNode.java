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
package org.sosy_lab.cpachecker.cfa.model;

/**
 * A CFANode that marks the end of a path.
 */
public class CFATerminationNode extends CFANode {

  public CFATerminationNode(String pFunctionName) {
    super(pFunctionName);
  }

  @Override
  public void addLeavingEdge(CFAEdge pNewLeavingEdge) {
    throw new AssertionError(pNewLeavingEdge);
  }

  @Override
  public void addLeavingSummaryEdge(FunctionSummaryEdge pEdge) {
    throw new AssertionError(pEdge);
  }
}
