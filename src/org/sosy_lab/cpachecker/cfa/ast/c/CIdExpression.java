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

import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import java.util.Objects;

public final class CIdExpression extends AIdExpression implements CLeftHandSide {


  public CIdExpression(
      final FileLocation pFileLocation,
      final CType pType, final String pName,
      final CSimpleDeclaration pDeclaration) {
    super(pFileLocation, pType, pName, pDeclaration);
  }

  public CIdExpression(final FileLocation pFileLocation, final CSimpleDeclaration pDeclaration) {
    super(pFileLocation, pDeclaration);
  }

  public static CIdExpression DUMMY_ID(CType type) {
    return new CIdExpression(FileLocation.DUMMY, CVariableDeclaration.DUMMY(type));
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
  }

  /**
   * Get the declaration of the variable.
   * The result may be null if the variable was not declared.
   */
  @Override
  public CSimpleDeclaration getDeclaration() {
    return (CSimpleDeclaration) super.getDeclaration();
  }

  @Override
  public <R, X extends Exception> R accept(CExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CLeftHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    if (getDeclaration() != null) {
      result = prime * result + Objects.hash(getDeclaration().getQualifiedName());
    }
    return prime * result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CIdExpression)) {
      return false;
    }

    // Don't call super.equals() here,
    // it compares the declaration field.
    // In C, there might be several declarations declaring the same variable,
    // so we sometimes need to return true even with different declarations.

    CIdExpression other = (CIdExpression) obj;

    if (getDeclaration() == null) {
      return other.getDeclaration() == null;
    } else {
      return Objects
          .equals(getDeclaration().getQualifiedName(), other.getDeclaration().getQualifiedName());
    }
  }
}