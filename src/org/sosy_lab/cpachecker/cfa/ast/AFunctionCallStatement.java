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

import java.util.Objects;


public abstract class AFunctionCallStatement extends AbstractStatement implements AFunctionCall {

  private final AFunctionCallExpression functionCall;

  public AFunctionCallStatement(FileLocation pFileLocation, AFunctionCallExpression pFunctionCall) {
    super(pFileLocation);
    functionCall = pFunctionCall;
  }

  @Override
  public <R, X extends Exception> R accept(AStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString() {
    return functionCall.toASTString() + ";";
  }

  @Override
  public AFunctionCallExpression getFunctionCallExpression() {
    return functionCall;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(functionCall);
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

    if (!(obj instanceof AFunctionCallStatement)
        || !super.equals(obj)) {
      return false;
    }

    AFunctionCallStatement other = (AFunctionCallStatement) obj;

    return Objects.equals(other.functionCall, functionCall);
  }

}