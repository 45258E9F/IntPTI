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

import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;


/**
 * AST node for the expression "a = b".
 */
public class CExpressionAssignmentStatement extends AExpressionAssignmentStatement
    implements CAssignment, CStatement {

  public CExpressionAssignmentStatement(
      FileLocation pFileLocation,
      CLeftHandSide pLeftHandSide,
      CExpression pRightHandSide) {
    super(pFileLocation, pLeftHandSide, pRightHandSide);

  }

  @Override
  public CLeftHandSide getLeftHandSide() {
    return (CLeftHandSide) super.getLeftHandSide();
  }

  @Override
  public CExpression getRightHandSide() {
    return (CExpression) super.getRightHandSide();
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

    if (!(obj instanceof CExpressionAssignmentStatement)) {
      return false;
    }

    return super.equals(obj);
  }
}
