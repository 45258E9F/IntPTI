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
package org.sosy_lab.cpachecker.cfa.ast.c;

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.ast.AbstractReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public class CReturnStatement extends AbstractReturnStatement implements CAstNode {

  public CReturnStatement(
      final FileLocation pFileLocation,
      final Optional<CExpression> pExpression,
      final Optional<CAssignment> pAssignment) {
    super(pFileLocation, pExpression, pAssignment);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @SuppressWarnings("unchecked") // safe because Optional is covariant
  @Override
  public Optional<CExpression> getReturnValue() {
    return (Optional<CExpression>) super.getReturnValue();
  }

  @Override
  @SuppressWarnings("unchecked") // safe because Optional is covariant
  public Optional<CAssignment> asAssignment() {
    return (Optional<CAssignment>) super.asAssignment();
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

    if (!(obj instanceof CReturnStatement)) {
      return false;
    }

    return super.equals(obj);
  }
}
