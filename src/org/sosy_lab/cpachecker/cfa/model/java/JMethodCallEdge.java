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
package org.sosy_lab.cpachecker.cfa.model.java;

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodOrConstructorInvocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;

import java.util.List;

public class JMethodCallEdge extends FunctionCallEdge {


  public JMethodCallEdge(
      String pRawStatement,
      FileLocation pFileLocation, CFANode pPredecessor, JMethodEntryNode pSuccessor,
      JMethodOrConstructorInvocation pFunctionCall, JMethodSummaryEdge pSummaryEdge) {

    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor, pFunctionCall, pSummaryEdge);

  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.FunctionCallEdge;
  }

  @Override
  public JMethodSummaryEdge getSummaryEdge() {
    return (JMethodSummaryEdge) summaryEdge;
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<JExpression> getArguments() {
    return (List<JExpression>) functionCall.getFunctionCallExpression().getParameterExpressions();
  }

  @Override
  public String getCode() {
    return functionCall.getFunctionCallExpression().toASTString();
  }

  @Override
  public Optional<JMethodOrConstructorInvocation> getRawAST() {
    return Optional.of((JMethodOrConstructorInvocation) functionCall);
  }

  @Override
  public JMethodEntryNode getSuccessor() {
    // the constructor enforces that the successor is always a FunctionEntryNode
    return (JMethodEntryNode) super.getSuccessor();
  }
}