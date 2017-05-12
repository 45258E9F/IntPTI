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


import org.sosy_lab.cpachecker.cfa.types.Type;

import java.util.Objects;

/**
 * Abstract Super class for all possible right-hand sides of an assignment.
 * This class is only SuperClass of all abstract Classes and their Subclasses.
 * The Interface {@link ARightHandSide} contains all language specific
 * AST Nodes as well.
 */
public abstract class AbstractRightHandSide extends AbstractAstNode implements ARightHandSide {

  private final Type type;

  public AbstractRightHandSide(FileLocation pFileLocation, Type pType) {
    super(pFileLocation);
    type = pType;
  }

  @Override
  public Type getExpressionType() {
    return type;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(type);
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

    if (!(obj instanceof AbstractRightHandSide)
        || !super.equals(obj)) {
      return false;
    }

    AbstractRightHandSide other = (AbstractRightHandSide) obj;

    return Objects.equals(other.type, type);
  }


}
