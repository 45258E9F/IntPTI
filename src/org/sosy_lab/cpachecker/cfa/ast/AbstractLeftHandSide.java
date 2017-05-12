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
 * Abstract class for side-effect free expressions.
 * This class is only SuperClass of all abstract Classes and their Subclasses.
 * The Interface {@link org.sosy_lab.cpachecker.cfa.ast.AExpression} contains all language specific
 * AST Nodes as well.
 */
public abstract class AbstractLeftHandSide extends AbstractExpression implements ALeftHandSide {

  public AbstractLeftHandSide(FileLocation pFileLocation, Type pType) {
    super(pFileLocation, pType);
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

    if (!(obj instanceof AbstractLeftHandSide)) {
      return false;
    }

    return super.equals(obj);
  }
}
