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
package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;

import java.util.List;

import javax.annotation.Nullable;

/**
 * This class represents the class instance creation expression AST node type.
 *
 * ClassInstanceCreation:
 * [ Expression . ]
 * new [ < Type { , Type } > ]
 * Type ( [ Expression { , Expression } ] )
 * [ AnonymousClassDeclaration ]
 *
 * The functionname is in most cases a {@link JIdExpression}.
 *
 * Not all node arragements will represent legal Java constructs.
 * In particular, it is nonsense if the functionname does not contain a {@link JIdExpression}.
 */
public class JClassInstanceCreation extends JMethodInvocationExpression implements JRightHandSide {

  //TODO Type Variables , AnonymousClassDeclaration

  public JClassInstanceCreation(
      FileLocation pFileLocation,
      JClassOrInterfaceType pType,
      JExpression pFunctionName,
      List<? extends JExpression> pParameters,
      JConstructorDeclaration pDeclaration) {

    super(pFileLocation, pType, pFunctionName, pParameters, pDeclaration);
  }

  @Override
  @Nullable
  public JConstructorDeclaration getDeclaration() {
    return (JConstructorDeclaration) super.getDeclaration();
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

    StringBuilder astString = new StringBuilder("new ");
    astString.append(getExpressionType().toASTString(getFunctionNameExpression().toASTString()));

    return astString.toString();
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

    if (!(obj instanceof JClassInstanceCreation)) {
      return false;
    }

    return super.equals(obj);
  }
}