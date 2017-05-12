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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;

import java.util.Objects;

public abstract class AbstractReturnStatement extends AbstractAstNode implements AReturnStatement {

  private final Optional<? extends AExpression> expression;
  private final Optional<? extends AAssignment> assignment;

  public AbstractReturnStatement(
      final FileLocation pFileLocation,
      final Optional<? extends AExpression> pExpression,
      final Optional<? extends AAssignment> pAssignment) {
    super(pFileLocation);
    expression = checkNotNull(pExpression);
    assignment = checkNotNull(pAssignment);
  }

  @Override
  public String toASTString() {
    return "return"
        + (expression.isPresent() ? " " + expression.get().toASTString() : "")
        + ";";
  }

  @Override
  public Optional<? extends AExpression> getReturnValue() {
    return expression;
  }

  @Override
  public Optional<? extends AAssignment> asAssignment() {
    return assignment;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(expression);
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

    if (!(obj instanceof AbstractReturnStatement)
        || !super.equals(obj)) {
      return false;
    }

    AbstractReturnStatement other = (AbstractReturnStatement) obj;

    return Objects.equals(other.expression, expression);
  }

}