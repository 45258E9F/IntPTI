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

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;


public class AStatementEdge extends AbstractCFAEdge {

  protected final AStatement statement;

  protected AStatementEdge(
      String pRawStatement, AStatement pStatement,
      FileLocation pFileLocation, CFANode pPredecessor, CFANode pSuccessor) {

    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor);
    statement = pStatement;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.StatementEdge;
  }

  public AStatement getStatement() {
    return statement;
  }

  @Override
  public Optional<? extends AStatement> getRawAST() {
    return Optional.of(statement);
  }

  @Override
  public String getCode() {
    return statement.toASTString();
  }

}
