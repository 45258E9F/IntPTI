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

import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import java.util.Objects;

public final class CFieldReference extends AbstractExpression implements CLeftHandSide {

  private final String name;
  private final CExpression owner;
  private final boolean isPointerDereference;

  public CFieldReference(
      final FileLocation pFileLocation,
      final CType pType,
      final String pName,
      final CExpression pOwner,
      final boolean pIsPointerDereference) {
    super(pFileLocation, pType);
    name = pName;
    owner = pOwner;
    isPointerDereference = pIsPointerDereference;

    assert checkFieldAccess();
  }

  private boolean checkFieldAccess() throws IllegalArgumentException {
    CType structType = owner.getExpressionType().getCanonicalType();
    if (structType instanceof CCompositeType) {
      boolean found = false;
      for (CCompositeTypeMemberDeclaration field : ((CCompositeType) structType).getMembers()) {
        if (field.getName().equals(name)) {
          found = true;
          break;
        }
      }
      if (!found) {
        throw new IllegalArgumentException("Accessing unknown field " + name + " in " + structType);
      }
    }
    return true;
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
  }

  public String getFieldName() {
    return name;
  }

  public CExpression getFieldOwner() {
    return owner;
  }

  public boolean isPointerDereference() {
    return isPointerDereference;
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
  public String toASTString() {
    String left =
        (owner instanceof CFieldReference) ? owner.toASTString() : owner.toParenthesizedASTString();
    String op = isPointerDereference ? "->" : ".";
    return left + op + name;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + (isPointerDereference ? 1231 : 1237);
    result = prime * result + Objects.hashCode(name);
    result = prime * result + Objects.hashCode(owner);
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

    if (!(obj instanceof CFieldReference)
        || !super.equals(obj)) {
      return false;
    }

    CFieldReference other = (CFieldReference) obj;

    return Objects.equals(other.isPointerDereference, isPointerDereference)
        && Objects.equals(other.name, name)
        && Objects.equals(other.owner, owner);
  }

}
