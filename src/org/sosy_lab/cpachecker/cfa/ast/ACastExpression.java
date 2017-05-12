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

import org.sosy_lab.cpachecker.cfa.types.Type;

import java.util.Objects;

/**
 * This is the abstract Class for  Casted Expressions.
 */
public abstract class ACastExpression extends AbstractLeftHandSide {

  private final AExpression operand;
  private final Type castType;


  public ACastExpression(
      FileLocation pFileLocation,
      Type castExpressionType,
      AExpression pOperand) {
    super(pFileLocation, castExpressionType);

    operand = pOperand;
    castType = castExpressionType;
  }

  public AExpression getOperand() {
    return operand;
  }

  @Override
  public String toASTString() {
    return "(" + getExpressionType().toASTString("") + ")" + operand.toParenthesizedASTString();
  }

  public Type getCastType() {
    return castType;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(castType);
    result = prime * result + Objects.hashCode(operand);
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

    if (!(obj instanceof ACastExpression)
        || !super.equals(obj)) {
      return false;
    }

    ACastExpression other = (ACastExpression) obj;

    return Objects.equals(other.operand, operand)
        && Objects.equals(other.castType, castType);
  }

}
