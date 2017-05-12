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

import com.google.common.base.Strings;

import org.sosy_lab.cpachecker.cfa.types.Type;

import java.util.Objects;


/**
 * This interface represents the core components that occur in each declaration:
 * a type and an (optional) name.
 *
 * This class is only SuperClass of all abstract Classes and their Subclasses.
 * The Interface {@link ASimpleDeclaration} contains all language specific
 * AST Nodes as well.
 */
public abstract class AbstractSimpleDeclaration extends AbstractAstNode
    implements ASimpleDeclaration {

  private Type type;
  private final String name;
  private final String origName;

  public AbstractSimpleDeclaration(
      FileLocation pFileLocation,
      final Type pType,
      final String pName,
      final String pOrigName) {
    super(pFileLocation);
    type = pType;
    name = pName;
    origName = pOrigName;
  }

  public AbstractSimpleDeclaration(
      final FileLocation pFileLocation,
      final Type pType, final String pName) {
    this(pFileLocation, pType, pName, pName);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getOrigName() {
    return origName;
  }

  @Override
  public String toASTString() {
    String name = Strings.nullToEmpty(getName());
    return getType().toASTString(name) + ";";
  }

  @Override
  public Type getType() {
    return type;
  }

  protected void setType(Type pType) {
    type = pType;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(type);
    result = prime * result + Objects.hashCode(name);
    result = prime * result + Objects.hashCode(origName);
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

    if (!(obj instanceof AbstractSimpleDeclaration)
        || !super.equals(obj)) {
      return false;
    }

    AbstractSimpleDeclaration other = (AbstractSimpleDeclaration) obj;

    return Objects.equals(other.type, type)
        && Objects.equals(other.name, name)
        && Objects.equals(other.origName, origName);
  }

}