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
package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

import javax.annotation.Nullable;

/**
 * Class for expressions that represent names of declared constructs.
 * ( e.g. variables, names of methods in Invocation).
 *
 * If possible, it saves a reference to the declaration this name references.
 */
public class JIdExpression extends AIdExpression implements JLeftHandSide {

  public JIdExpression(
      FileLocation pFileLocation,
      JType pType,
      String pName,
      JSimpleDeclaration pDeclaration) {
    super(pFileLocation, pType, pName, pDeclaration);
    // TODO Refactor, so we do not need null for declaration.
    // (Insert extra classes or objects for unresolvable declarations)
    //assert pDeclaration != null;
  }

  @Override
  @Nullable
  public JSimpleDeclaration getDeclaration() {
    return (JSimpleDeclaration) super.getDeclaration();
  }

  @Override
  public JType getExpressionType() {
    return (JType) super.getExpressionType();
  }

  @Override
  public <R, X extends Exception> R accept(JRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(JLeftHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
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

    if (!(obj instanceof JIdExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}