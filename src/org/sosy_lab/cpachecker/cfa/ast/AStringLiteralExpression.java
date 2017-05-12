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


public abstract class AStringLiteralExpression extends ALiteralExpression {

  private final String value;

  public AStringLiteralExpression(FileLocation pFileLocation, Type pType, String pValue) {
    super(pFileLocation, pType);
    value = pValue;
  }

  @Override
  public String toASTString() {
    return value;
  }

  @Override
  public String getValue() {
    return value;
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

    if (!(obj instanceof AStringLiteralExpression)
        || !super.equals(obj)) {
      return false;
    }

    AStringLiteralExpression other = (AStringLiteralExpression) obj;

    return Objects.equals(other.value, value);
  }

}
