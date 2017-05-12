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
package org.sosy_lab.cpachecker.cfa.ast;


import org.sosy_lab.cpachecker.cfa.types.Type;

/**
 * This interface represents all sorts of top-level declarations (i.e., declarations
 * not nested inside another type declaration).
 * This excludes for examples function parameter declarations and struct members.
 * It includes local and global variables and types, as well as functions.
 * This class is only SuperClass of all abstract Classes and their Subclasses.
 * The Interface {@link ADeclaration} contains all language specific
 * AST Nodes as well.
 */
public abstract class AbstractDeclaration extends AbstractSimpleDeclaration
    implements ADeclaration {

  private final boolean isGlobal;

  public AbstractDeclaration(
      FileLocation pFileLocation,
      boolean pIsGlobal,
      Type pType,
      String pName) {
    super(pFileLocation, pType, pName, pName);
    isGlobal = pIsGlobal;
  }

  public AbstractDeclaration(
      FileLocation pFileLocation,
      boolean pIsGlobal,
      Type pType,
      String pName,
      String pOrigName) {
    super(pFileLocation, pType, pName, pOrigName);
    isGlobal = pIsGlobal;
  }

  @Override
  public boolean isGlobal() {
    return isGlobal;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + (isGlobal ? 1231 : 1237);
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

    if (!(obj instanceof AbstractDeclaration)
        || !super.equals(obj)) {
      return false;
    }

    AbstractDeclaration other = (AbstractDeclaration) obj;

    return other.isGlobal == isGlobal;
  }

}