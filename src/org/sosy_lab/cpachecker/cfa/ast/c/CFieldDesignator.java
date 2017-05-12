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

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

import java.util.Objects;


public class CFieldDesignator extends CDesignator {

  private final String name;

  public CFieldDesignator(
      final FileLocation pFileLocation,
      final String pName) {
    super(pFileLocation);
    name = pName;
  }


  public String getFieldName() {
    return name;
  }

  @Override
  public String toASTString() {
    return "." + name;
  }

  @Override
  public String toParenthesizedASTString() {
    return toASTString();
  }

  @Override
  public <R, X extends Exception> R accept(CDesignatorVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
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

    if (!(obj instanceof CFieldDesignator)
        || !super.equals(obj)) {
      return false;
    }

    CFieldDesignator other = (CFieldDesignator) obj;

    return Objects.equals(other.name, name);
  }


}
