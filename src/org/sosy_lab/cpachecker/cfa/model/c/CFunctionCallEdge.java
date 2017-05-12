/*
 * Tsmart-BD: The static analysis component of Tsmart platform
 *
 * Copyright (C) 2013-2017  Tsinghua University
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

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;

import java.util.List;

public class CFunctionCallEdge extends FunctionCallEdge {


  public CFunctionCallEdge(
      String pRawStatement,
      FileLocation pFileLocation, CFANode pPredecessor, CFunctionEntryNode pSuccessor,
      CFunctionCall pFunctionCall, CFunctionSummaryEdge pSummaryEdge) {

    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor, pFunctionCall, pSummaryEdge);

  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.FunctionCallEdge;
  }

  @Override
  public CFunctionSummaryEdge getSummaryEdge() {
    return (CFunctionSummaryEdge) summaryEdge;
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<CExpression> getArguments() {
    return (List<CExpression>) functionCall.getFunctionCallExpression().getParameterExpressions();
  }

  @Override
  public String getCode() {
    return functionCall.getFunctionCallExpression().toASTString();
  }

  @Override
  public Optional<CFunctionCall> getRawAST() {
    return Optional.of((CFunctionCall) functionCall);
  }

  @Override
  public CFunctionEntryNode getSuccessor() {
    // the constructor enforces that the successor is always a FunctionEntryNode
    return (CFunctionEntryNode) super.getSuccessor();
  }
}