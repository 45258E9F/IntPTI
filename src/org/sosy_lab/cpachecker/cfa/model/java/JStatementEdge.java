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
import org.sosy_lab.cpachecker.cfa.ast.java.JStatement;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class JStatementEdge extends AStatementEdge {


  public JStatementEdge(
      String pRawStatement, JStatement pStatement,
      FileLocation pFileLocation, CFANode pPredecessor, CFANode pSuccessor) {

    super(pRawStatement, pStatement, pFileLocation, pPredecessor, pSuccessor);
  }


  @Override
  public JStatement getStatement() {
    return (JStatement) statement;
  }

  @Override
  public Optional<JStatement> getRawAST() {
    return Optional.of((JStatement) statement);
  }
}
