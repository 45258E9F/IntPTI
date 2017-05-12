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

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

import java.util.List;


public class FunctionCallEdge extends AbstractCFAEdge {

  protected final AFunctionCall functionCall;
  protected final FunctionSummaryEdge summaryEdge;


  protected FunctionCallEdge(
      String pRawStatement, FileLocation pFileLocation, CFANode pPredecessor, CFANode pSuccessor,
      AFunctionCall pFunctionCall, FunctionSummaryEdge pSummaryEdge) {
    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor);
    functionCall = pFunctionCall;
    summaryEdge = pSummaryEdge;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.FunctionCallEdge;
  }

  public FunctionSummaryEdge getSummaryEdge() {
    return summaryEdge;
  }


  public List<? extends AExpression> getArguments() {
    return functionCall.getFunctionCallExpression().getParameterExpressions();
  }

  @Override
  public String getCode() {
    return functionCall.getFunctionCallExpression().toASTString();
  }

  @Override
  public Optional<? extends AFunctionCall> getRawAST() {
    return Optional.of(functionCall);
  }

  @Override
  public FunctionEntryNode getSuccessor() {
    // the constructor enforces that the successor is always a FunctionEntryNode
    return (FunctionEntryNode) super.getSuccessor();
  }
}