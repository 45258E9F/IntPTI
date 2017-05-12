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

import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;

import java.util.List;

/**
 * This class represents forward declarations of functions.
 * Example code:
 *
 * int foo(int x);
 */
public final class CFunctionDeclaration extends AFunctionDeclaration implements CDeclaration {

  public CFunctionDeclaration(
      FileLocation pFileLocation,
      CFunctionType pType, String pName,
      List<CParameterDeclaration> parameters) {
    super(pFileLocation, pType, checkNotNull(pName), parameters);
  }

  @Override
  public CFunctionType getType() {
    return (CFunctionType) super.getType();
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<CParameterDeclaration> getParameters() {
    return (List<CParameterDeclaration>) super.getParameters();
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CFunctionDeclaration)) {
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
