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

import java.math.BigDecimal;
import java.util.Objects;


public abstract class AFloatLiteralExpression extends ALiteralExpression {

  private final BigDecimal value;

  public AFloatLiteralExpression(FileLocation pFileLocation, Type pType, BigDecimal pValue) {
    super(pFileLocation, pType);
    value = pValue;
  }

  @Override
  public BigDecimal getValue() {
    return value;
  }

  @Override
  public String toASTString() {
    return value.toString();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(value);
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

    if (!(obj instanceof AFloatLiteralExpression)
        || !super.equals(obj)) {
      return false;
    }

    AFloatLiteralExpression other = (AFloatLiteralExpression) obj;

    return Objects.equals(other.value, value);
  }

}
