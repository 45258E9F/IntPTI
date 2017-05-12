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


public abstract class AArraySubscriptExpression extends AbstractLeftHandSide {


  private final AExpression arrayExpression;
  private final AExpression subscriptExpression;

  public AArraySubscriptExpression(
      FileLocation pFileLocation,
      Type pType,
      final AExpression pArrayExpression,
      final AExpression pSubscriptExpression) {
    super(pFileLocation, pType);
    arrayExpression = pArrayExpression;
    subscriptExpression = pSubscriptExpression;

  }

  public AExpression getArrayExpression() {
    return arrayExpression;
  }

  public AExpression getSubscriptExpression() {
    return subscriptExpression;
  }

  @Override
  public String toASTString() {
    String left =
        (arrayExpression instanceof AArraySubscriptExpression) ? arrayExpression.toASTString()
                                                               : arrayExpression
            .toParenthesizedASTString();
    return left + "[" + subscriptExpression.toASTString() + "]";
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(arrayExpression);
    result = prime * result + Objects.hashCode(subscriptExpression);
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

    if (!(obj instanceof AArraySubscriptExpression)
        || !super.equals(obj)) {
      return false;
    }

    AArraySubscriptExpression other = (AArraySubscriptExpression) obj;

    return Objects.equals(other.arrayExpression, arrayExpression)
        && Objects.equals(other.subscriptExpression, subscriptExpression);
  }

}