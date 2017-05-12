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
package org.sosy_lab.cpachecker.cfa.model.c;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

public class CFunctionSummaryEdge extends FunctionSummaryEdge {


  public CFunctionSummaryEdge(
      String pRawStatement, FileLocation pFileLocation,
      CFANode pPredecessor, CFANode pSuccessor, CFunctionCall pExpression,
      CFunctionEntryNode pFunctionEntry) {

    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor, pExpression,
        pFunctionEntry);
  }

  @Override
  public CFunctionCall getExpression() {
    return (CFunctionCall) super.getExpression();
  }

  @Override
  public CFunctionEntryNode getFunctionEntry() {
    return (CFunctionEntryNode) super.getFunctionEntry();
  }
}