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
package org.sosy_lab.cpachecker.cfa.ast;

import org.sosy_lab.cpachecker.cfa.types.Type;

import java.util.Objects;

public abstract class AUnaryExpression extends AbstractExpression {

  private final AExpression operand;
  private final AUnaryOperator operator;

  public AUnaryExpression(
      FileLocation pFileLocation, Type pType, final AExpression pOperand,
      final AUnaryOperator pOperator) {
    super(pFileLocation, pType);
    operand = pOperand;
    operator = pOperator;
  }

  public AExpression getOperand() {
    return operand;
  }

  public AUnaryOperator getOperator() {
    return operator;
  }

  @Override
  public String toASTString() {
    return operator.getOperator() + operand.toParenthesizedASTString();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hash(operand);
    result = prime * result + Objects.hash(operator);
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

    if (!(obj instanceof AUnaryExpression)
        || !super.equals(obj)) {
      return false;
    }

    AUnaryExpression other = (AUnaryExpression) obj;

    return Objects.equals(other.operand, operand)
        && Objects.equals(other.operator, operator);
  }

  public static interface AUnaryOperator {
    /**
     * Returns the string representation of this operator (e.g. "*", "+").
     */
    public String getOperator();
  }

}