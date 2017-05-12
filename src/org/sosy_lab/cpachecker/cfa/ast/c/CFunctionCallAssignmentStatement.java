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
package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;


public final class CFunctionCallAssignmentStatement extends AFunctionCallAssignmentStatement
    implements CStatement, CAssignment, CFunctionCall {

  public CFunctionCallAssignmentStatement(
      FileLocation pFileLocation,
      CLeftHandSide pLeftHandSide,
      CFunctionCallExpression pRightHandSide) {
    super(pFileLocation, pLeftHandSide, pRightHandSide);
  }

  @Override
  public CLeftHandSide getLeftHandSide() {
    return (CLeftHandSide) super.getLeftHandSide();
  }

  @Override
  public CFunctionCallExpression getRightHandSide() {
    return (CFunctionCallExpression) super.getRightHandSide();
  }

  @Override
  public CFunctionCallExpression getFunctionCallExpression() {
    return (CFunctionCallExpression) super.getFunctionCallExpression();
  }

  @Override
  public <R, X extends Exception> R accept(CStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public String toASTString() {
    return getLeftHandSide().toASTString()
        + " = " + getRightHandSide().toASTString() + ";";
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CFunctionCallAssignmentStatement)) {
      return false;
    }

    return super.equals(obj);
  }

}
