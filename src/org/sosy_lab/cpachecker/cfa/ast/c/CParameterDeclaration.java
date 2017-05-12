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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import java.util.Objects;


/**
 * This is the declaration of a function parameter. It contains a type and a name.
 */
public final class CParameterDeclaration extends AParameterDeclaration
    implements CSimpleDeclaration {

  private String qualifiedName;

  public CParameterDeclaration(
      FileLocation pFileLocation,
      CType pType,
      String pName) {
    super(pFileLocation, pType, checkNotNull(pName));
  }

  public void setQualifiedName(String pQualifiedName) {
    checkState(qualifiedName == null);
    qualifiedName = checkNotNull(pQualifiedName);
  }

  @Override
  public String getQualifiedName() {
    return qualifiedName;
  }

  @Override
  public CType getType() {
    return (CType) super.getType();
  }

  public CVariableDeclaration asVariableDeclaration() {
    return new CVariableDeclaration(getFileLocation(), false, CStorageClass.AUTO,
        getType(), getName(), getOrigName(), getQualifiedName(), null);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    result = prime * result + super.hashCode();
    result = prime * result + Objects.hashCode(qualifiedName);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CParameterDeclaration) || !super.equals(obj)) {
      return false;
    }

    CParameterDeclaration other = (CParameterDeclaration) obj;
    return Objects.equals(qualifiedName, other.qualifiedName);
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