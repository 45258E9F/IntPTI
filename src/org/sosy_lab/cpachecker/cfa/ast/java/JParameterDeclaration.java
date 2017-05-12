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
package org.sosy_lab.cpachecker.cfa.ast.java;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * This class contains Parameter Declarations for methods.
 * It contains a type and a name.
 */
public class JParameterDeclaration extends AParameterDeclaration implements JSimpleDeclaration {

  private final String qualifiedName;
  private final boolean isFinal;

  public JParameterDeclaration(
      FileLocation pFileLocation, JType pType,
      String pName, String pQualifiedName, boolean pIsFinal) {
    super(pFileLocation, pType, pName);
    qualifiedName = checkNotNull(pQualifiedName);
    isFinal = pIsFinal;
  }

  @Override
  public JType getType() {
    return (JType) super.getType();
  }

  public boolean isFinal() {
    return isFinal;
  }

  @Override
  public String getQualifiedName() {
    return qualifiedName;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + (isFinal ? 1231 : 1237);
    result = prime * result + qualifiedName.hashCode();
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

    if (!(obj instanceof JParameterDeclaration)
        || !super.equals(obj)) {
      return false;
    }

    JParameterDeclaration other = (JParameterDeclaration) obj;

    return other.isFinal == isFinal
        && qualifiedName.equals(other.qualifiedName);
  }

}
