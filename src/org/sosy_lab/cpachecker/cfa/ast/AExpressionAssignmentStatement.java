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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;


public abstract class AExpressionAssignmentStatement extends AbstractStatement
    implements AAssignment {

  private final ALeftHandSide leftHandSide;
  private final AExpression rightHandSide;

  public AExpressionAssignmentStatement(
      FileLocation pFileLocation, ALeftHandSide pLeftHandSide,
      AExpression pRightHandSide) {
    super(pFileLocation);
    leftHandSide = checkNotNull(pLeftHandSide);
    rightHandSide = checkNotNull(pRightHandSide);
  }

  @Override
  public String toASTString() {
    return leftHandSide.toASTString()
        + " = " + rightHandSide.toASTString() + ";";
  }

  @Override
  public ALeftHandSide getLeftHandSide() {
    return leftHandSide;
  }

  @Override
  public AExpression getRightHandSide() {
    return rightHandSide;
  }

  @Override
  public <R, X extends Exception> R accept(AStatementVisitor<R, X> pV) throws X {
    return pV.visit(this);
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

    if (!(obj instanceof AExpressionAssignmentStatement)
        || !super.equals(obj)) {
      return false;
    }

    AExpressionAssignmentStatement other = (AExpressionAssignmentStatement) obj;

    return Objects.equals(other.leftHandSide, leftHandSide)
        && Objects.equals(other.rightHandSide, rightHandSide);
  }

}
