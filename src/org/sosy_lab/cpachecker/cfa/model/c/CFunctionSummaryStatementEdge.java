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

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class CFunctionSummaryStatementEdge extends CStatementEdge {
  private final String functionName;
  private final CFunctionCall fcall;

  public CFunctionSummaryStatementEdge(
      String pRawStatement,
      CStatement pStatement, FileLocation pFileLocation, CFANode pPredecessor,
      CFANode pSuccessor, CFunctionCall fcall, String functionName) {
    super(pRawStatement, pStatement, pFileLocation, pPredecessor, pSuccessor);
    this.functionName = checkNotNull(functionName);
    this.fcall = checkNotNull(fcall);
  }

  public String getFunctionName() {
    return functionName;
  }

  public CFunctionCall getFunctionCall() {
    return fcall;
  }
}