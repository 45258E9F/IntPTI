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

/**
 * This is the abstract Class for Character Literals.
 */
public abstract class ACharLiteralExpression extends ALiteralExpression {


  private final char character;

  public ACharLiteralExpression(FileLocation pFileLocation, Type pType, char pCharacter) {
    super(pFileLocation, pType);
    character = pCharacter;
  }

  public char getCharacter() {
    return character;
  }

  @Override
  public Character getValue() {
    return getCharacter();
  }

  @Override
  public String toASTString() {
    if (character >= ' ' && character < 128) {
      return "'" + character + "'";
    } else {
      return "'\\x" + Integer.toHexString(character) + "'";
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + character;
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

    if (!(obj instanceof ACharLiteralExpression)
        || !super.equals(obj)) {
      return false;
    }

    ACharLiteralExpression other = (ACharLiteralExpression) obj;

    return other.character == character;
  }

}
