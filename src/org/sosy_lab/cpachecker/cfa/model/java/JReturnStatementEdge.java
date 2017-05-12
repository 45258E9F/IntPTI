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
import org.sosy_lab.cpachecker.cfa.ast.java.JAssignment;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JReturnStatement;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

public class JReturnStatementEdge extends AReturnStatementEdge {


  public JReturnStatementEdge(
      String pRawStatement, JReturnStatement pRawAST,
      FileLocation pFileLocation, CFANode pPredecessor, FunctionExitNode pSuccessor) {

    super(pRawStatement, pRawAST, pFileLocation, pPredecessor, pSuccessor);

  }

  @SuppressWarnings("unchecked") // safe because Optional is covariant
  @Override
  public Optional<JExpression> getExpression() {
    return (Optional<JExpression>) rawAST.getReturnValue();
  }

  @SuppressWarnings("unchecked") // safe because Optional is covariant
  @Override
  public Optional<JAssignment> asAssignment() {
    return (Optional<JAssignment>) super.asAssignment();
  }

  @Override
  public Optional<JReturnStatement> getRawAST() {
    return Optional.of((JReturnStatement) rawAST);
  }

}
