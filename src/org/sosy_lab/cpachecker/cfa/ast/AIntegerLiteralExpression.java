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

import java.math.BigInteger;
import java.util.Objects;


public abstract class AIntegerLiteralExpression extends ALiteralExpression {

  private final BigInteger value;

  public AIntegerLiteralExpression(FileLocation pFileLocation, Type pType, BigInteger pValue) {
    super(pFileLocation, pType);
    value = pValue;
  }

  @Override
  public BigInteger getValue() {
    return value;
  }

  public long asLong() {
    // TODO handle values that are bigger than MAX_LONG
    return value.longValue();
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

    if (!(obj instanceof AIntegerLiteralExpression)
        || !super.equals(obj)) {
      return false;
    }

    AIntegerLiteralExpression other = (AIntegerLiteralExpression) obj;

    return Objects.equals(other.value, value);
  }

}