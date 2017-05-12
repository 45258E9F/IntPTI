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
package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.AbstractDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * This class represents declarations that define new types,
 * e.g.
 *
 * struct s { int i; }
 * typedef int my_int;
 */
public abstract class CTypeDeclaration extends AbstractDeclaration implements CDeclaration {

  private final String qualifiedName;

  public CTypeDeclaration(
      FileLocation pFileLocation, boolean pIsGlobal,
      CType pType, String pName, String pQualifiedName) {
    super(pFileLocation, pIsGlobal, pType, pName, pName);
    qualifiedName = pQualifiedName;
  }

  @Override
  public String getQualifiedName() {
    return qualifiedName;
  }

  @Override
  public CType getType() {
    return (CType) super.getType();
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (!(obj instanceof CTypeDeclaration)) {
      return false;
    }

    return super.equals(obj);
  }

}
