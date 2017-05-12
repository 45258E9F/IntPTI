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
package org.sosy_lab.cpachecker.util.access;

import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;

import java.util.Objects;


public class DeclarationSegment implements PathSegment {
  private final CSimpleDeclaration declaration;

  public DeclarationSegment(CSimpleDeclaration pDeclaration) {
    super();
    declaration = pDeclaration;
  }

  @Override
  public String getName() {
    return declaration.getQualifiedName();
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    }
    if (that == null || !getClass().equals(that.getClass())) {
      return false;
    }
    DeclarationSegment other = (DeclarationSegment) that;
    if (declaration instanceof CVariableDeclaration &&
        other.declaration instanceof CVariableDeclaration) {
      return ((CVariableDeclaration) declaration).equalsWithoutStorageClass(
          other.declaration);
    }
    return declaration.equals(other.declaration);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(declaration);
  }
}
