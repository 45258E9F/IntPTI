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
package org.sosy_lab.cpachecker.cfa.model;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;


public class FunctionSummaryEdge extends AbstractCFAEdge {

  private final AFunctionCall expression;
  private final FunctionEntryNode functionEntry;

  protected FunctionSummaryEdge(
      String pRawStatement, FileLocation pFileLocation,
      CFANode pPredecessor, CFANode pSuccessor,
      AFunctionCall pExpression, FunctionEntryNode pFunctionEntry) {
    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor);
    expression = pExpression;
    functionEntry = checkNotNull(pFunctionEntry);
  }

  public AFunctionCall getExpression() {
    return expression;
  }

  public FunctionEntryNode getFunctionEntry() {
    return functionEntry;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.CallToReturnEdge;
  }

  @Override
  public String getCode() {
    return expression.toASTString();
  }

}