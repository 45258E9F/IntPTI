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

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;

import java.util.List;

/**
 * This class represents the super constructor invocation statement AST node type.
 *
 * SuperConstructorInvocation:
 * [ Expression . ]
 * [ < Type { , Type } > ]
 * super ( [ Expression { , Expression } ] ) ;
 */
public class JSuperConstructorInvocation extends JClassInstanceCreation {

  public JSuperConstructorInvocation(
      FileLocation pFileLocation, JClassType pType, JExpression pFunctionName,
      List<? extends JExpression> pParameters, JConstructorDeclaration pDeclaration) {
    super(pFileLocation, pType, pFunctionName, pParameters, pDeclaration);

  }

  @Override
  public String toASTString() {
    return getExpressionType().toASTString("super");
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

    if (!(obj instanceof JSuperConstructorInvocation)) {
      return false;
    }

    return super.equals(obj);
  }
}
