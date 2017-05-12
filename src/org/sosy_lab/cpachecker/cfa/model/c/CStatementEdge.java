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
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class CStatementEdge extends AStatementEdge {


  public CStatementEdge(
      String pRawStatement, CStatement pStatement,
      FileLocation pFileLocation, CFANode pPredecessor, CFANode pSuccessor) {

    super(pRawStatement, pStatement, pFileLocation, pPredecessor, pSuccessor);
  }


  @Override
  public CStatement getStatement() {
    return (CStatement) statement;
  }

  @Override
  public Optional<CStatement> getRawAST() {
    return Optional.of((CStatement) statement);
  }
}
