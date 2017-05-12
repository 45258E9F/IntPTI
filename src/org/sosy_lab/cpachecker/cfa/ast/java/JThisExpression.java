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

import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;

/**
 * This Expression is used, if either the Run Time Type or Run Time Object
 * of the this (Keyword) Reference  is requested.
 * As part of a regular Expression, it denotes the Run Time Object. As Part of a
 * JRunTimeTypeEqualsType Expression, it denotes the Run Time Type.
 */
public class JThisExpression extends AbstractExpression implements JRunTimeTypeExpression {

  public JThisExpression(FileLocation pFileLocation, JClassOrInterfaceType pType) {
    super(pFileLocation, pType);
  }

  @Override
  public JClassOrInterfaceType getExpressionType() {
    return (JClassOrInterfaceType) super.getExpressionType();
  }

  @Override
  public <R, X extends Exception> R accept(JRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString() {
    return "this";
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public boolean isThisReference() {
    return true;
  }

  @Override
  public boolean isVariableReference() {
    return false;
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

    if (!(obj instanceof JThisExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}