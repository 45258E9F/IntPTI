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
package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * This class and its subclasses represent locale Variable declarations or Field declarations.
 *
 * e.g. Type a = b;
 */
public class JVariableDeclaration extends AVariableDeclaration implements JDeclaration {


  private static final boolean IS_LOCAL = false;
  private final boolean isFinal;

  protected JVariableDeclaration(
      FileLocation pFileLocation, boolean pIsGlobal, JType pType, String pName,
      String pOrigName, String pQualifiedName, AInitializer pInitializer, boolean pIsFinal) {
    super(pFileLocation, pIsGlobal, pType, pName, pOrigName, pQualifiedName, pInitializer);

    isFinal = pIsFinal;
  }

  public JVariableDeclaration(
      FileLocation pFileLocation, JType pType, String pName,
      String pOrigName, String pQualifiedName, AInitializer pInitializer, boolean pIsFinal) {
    super(pFileLocation, IS_LOCAL, pType, pName, pOrigName, pQualifiedName, pInitializer);

    isFinal = pIsFinal;
  }

  @Override
  public JType getType() {
    return (JType) super.getType();
  }

  @Override
  public String toASTString() {
    StringBuilder lASTString = new StringBuilder();

    if (isFinal) {
      lASTString.append("final ");
    }

    lASTString.append(getType().toASTString(getName()));

    if (getInitializer() != null) {
      lASTString.append(" = ");
      lASTString.append(getInitializer().toASTString());
    }

    lASTString.append(";");
    return lASTString.toString();
  }

  public boolean isFinal() {
    return isFinal;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + (isFinal ? 1231 : 1237);
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

    if (!(obj instanceof JVariableDeclaration)
        || !super.equals(obj)) {
      return false;
    }

    JVariableDeclaration other = (JVariableDeclaration) obj;

    return other.isFinal == isFinal;
  }

}