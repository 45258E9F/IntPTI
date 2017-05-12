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

public abstract class APointerExpression extends AbstractLeftHandSide {

  private final AExpression operand;

  public APointerExpression(FileLocation pFileLocation, Type pType, final AExpression pOperand) {
    super(pFileLocation, pType);
    operand = pOperand;
  }

  public AExpression getOperand() {
    return operand;
  }

  @Override
  public String toASTString() {
    return "*" + operand.toParenthesizedASTString();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hash(operand);
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

    if (!(obj instanceof APointerExpression)
        || !super.equals(obj)) {
      return false;
    }

    APointerExpression other = (APointerExpression) obj;

    return Objects.equals(other.operand, operand);
  }

}