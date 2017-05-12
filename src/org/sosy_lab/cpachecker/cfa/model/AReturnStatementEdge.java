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

import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;


public class AReturnStatementEdge extends AbstractCFAEdge {

  protected final AReturnStatement rawAST;

  protected AReturnStatementEdge(
      String pRawStatement, AReturnStatement pRawAST,
      FileLocation pFileLocation, CFANode pPredecessor, FunctionExitNode pSuccessor) {

    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor);
    rawAST = pRawAST;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.ReturnStatementEdge;
  }

  public Optional<? extends AExpression> getExpression() {
    return rawAST.getReturnValue();
  }

  /**
   * @see AReturnStatement#asAssignment()
   */
  public Optional<? extends AAssignment> asAssignment() {
    return rawAST.asAssignment();
  }

  @Override
  public Optional<? extends AReturnStatement> getRawAST() {
    return Optional.of(rawAST);
  }

  @Override
  public String getCode() {
    return rawAST.toASTString();
  }

  @Override
  public FunctionExitNode getSuccessor() {
    // the constructor enforces that the successor is always a FunctionExitNode
    return (FunctionExitNode) super.getSuccessor();
  }

}
