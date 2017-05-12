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
package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;

/**
 * This class represents declaration of complex types without declarations of
 * variables at the same time. Typedefs are not represented by this class.
 * Example code:
 *
 * struct s { ... };
 * struct s;
 * enum e { ... };
 *
 * TODO: As these declarations have no name, they should not be in the hierarchy
 * below {@link CSimpleDeclaration}.
 */
public final class CComplexTypeDeclaration extends CTypeDeclaration {

  public CComplexTypeDeclaration(
      FileLocation pFileLocation,
      boolean pIsGlobal, CComplexType pType) {
    super(pFileLocation, pIsGlobal, pType, null, null);
  }

  @Override
  public CComplexType getType() {
    return (CComplexType) super.getType();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    return result * prime + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CComplexTypeDeclaration)) {
      return false;
    }

    return super.equals(obj);
  }

  @Override
  public <R, X extends Exception> R accept(CSimpleDeclarationVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }
}
