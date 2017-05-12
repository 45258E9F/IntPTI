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

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.types.Type;


public abstract class AParameterDeclaration extends AbstractSimpleDeclaration {

  public AParameterDeclaration(FileLocation pFileLocation, Type pType, String pName) {
    super(pFileLocation, pType, checkNotNull(pName));

  }

  @Override
  public String toASTString() {
    return getType().toASTString(getName());
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

    if (!(obj instanceof AParameterDeclaration)) {
      return false;
    }

    return super.equals(obj);
  }

}