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
package org.sosy_lab.cpachecker.cfa.model.java;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodOrConstructorInvocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

public class JMethodSummaryEdge extends FunctionSummaryEdge {


  public JMethodSummaryEdge(
      String pRawStatement, FileLocation pFileLocation,
      CFANode pPredecessor, CFANode pSuccessor,
      JMethodOrConstructorInvocation pExpression, JMethodEntryNode pMethodEntry) {

    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor, pExpression,
        pMethodEntry);
  }

  @Override
  public JMethodOrConstructorInvocation getExpression() {
    return (JMethodOrConstructorInvocation) super.getExpression();
  }

  @Override
  public JMethodEntryNode getFunctionEntry() {
    return (JMethodEntryNode) super.getFunctionEntry();
  }
}