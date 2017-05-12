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
import org.sosy_lab.cpachecker.cfa.types.java.JType;

import java.util.List;
import java.util.Objects;

/**
 * This class represents the qualified method invocation expression AST node type.
 *
 * Qualified MethodInvocation:
 * <pre>
 *     Expression .
 *        [ < Type { , Type } > ]
 *        Identifier ( [ Expression { , Expression } ] )
 *  </pre>
 * Note that the qualification only consist of variables.
 * In the cfa, all method names are transformed to have unique names.
 */
public class JReferencedMethodInvocationExpression extends JMethodInvocationExpression {

  private final JIdExpression qualifier;

  public JReferencedMethodInvocationExpression(
      FileLocation pFileLocation,
      JType pType,
      JExpression pFunctionName,
      List<? extends JExpression> pParameters,
      JMethodDeclaration pDeclaration,
      JIdExpression pQualifier) {
    super(pFileLocation, pType, pFunctionName, pParameters, pDeclaration);
    qualifier = pQualifier;
  }

  public JIdExpression getReferencedVariable() {
    return qualifier;
  }

  @Override
  public String toASTString() {
    return qualifier.toASTString() + "_" + super.toASTString();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(qualifier);
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

    if (!(obj instanceof JReferencedMethodInvocationExpression)
        || super.equals(obj)) {
      return false;
    }

    JReferencedMethodInvocationExpression other = (JReferencedMethodInvocationExpression) obj;

    return Objects.equals(other.qualifier, qualifier);
  }

}
