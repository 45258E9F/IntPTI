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
package org.sosy_lab.cpachecker.cfa.ast;

import java.util.Objects;


public abstract class AFunctionCallAssignmentStatement extends AbstractStatement
    implements AAssignment, AFunctionCall {


  private final ALeftHandSide leftHandSide;
  private final AFunctionCallExpression rightHandSide;

  public AFunctionCallAssignmentStatement(
      FileLocation pFileLocation,
      ALeftHandSide pLeftHandSide,
      AFunctionCallExpression pRightHandSide) {
    super(pFileLocation);

    leftHandSide = pLeftHandSide;
    rightHandSide = pRightHandSide;

  }

  @Override
  public ALeftHandSide getLeftHandSide() {
    return leftHandSide;
  }


  @Override
  public AFunctionCallExpression getRightHandSide() {
    return rightHandSide;
  }


  @Override
  public AFunctionCallExpression getFunctionCallExpression() {
    return rightHandSide;
  }

  @Override
  public String toASTString() {
    return leftHandSide.toASTString()
        + " = " + rightHandSide.toASTString() + ";";
  }

  @Override
  public <R, X extends Exception> R accept(AStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(leftHandSide);
    result = prime * result + Objects.hashCode(rightHandSide);
    result = prime * result + super.hashCode();
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof AFunctionCallAssignmentStatement)
        || !super.equals(obj)) {
      return false;
    }

    AFunctionCallAssignmentStatement other = (AFunctionCallAssignmentStatement) obj;

    return Objects.equals(other.leftHandSide, leftHandSide)
        && Objects.equals(other.rightHandSide, rightHandSide);
  }

}