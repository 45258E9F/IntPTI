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


public abstract class AIdExpression extends AbstractLeftHandSide {

  private final String name;
  private final ASimpleDeclaration declaration;


  public AIdExpression(
      FileLocation pFileLocation, Type pType, final String pName,
      final ASimpleDeclaration pDeclaration) {
    super(pFileLocation, pType);
    name = pName.intern();
    declaration = pDeclaration;
  }


  public AIdExpression(FileLocation pFileLocation, ASimpleDeclaration pDeclaration) {
    this(pFileLocation, pDeclaration.getType(),
        pDeclaration.getName(), pDeclaration);
  }

  public String getName() {
    return name;
  }

  @Override
  public String toParenthesizedASTString() {
    return toASTString();
  }

  @Override
  public String toASTString() {
    return name;
  }

  public ASimpleDeclaration getDeclaration() {
    return declaration;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(declaration);
    result = prime * result + Objects.hashCode(name);
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

    if (!(obj instanceof AIdExpression)
        || !super.equals(obj)) {
      return false;
    }

    AIdExpression other = (AIdExpression) obj;

    return Objects.equals(other.declaration, declaration)
        && Objects.equals(other.name, name);
  }

}