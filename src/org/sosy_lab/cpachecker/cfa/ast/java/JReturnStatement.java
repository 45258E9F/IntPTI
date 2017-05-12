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

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.ast.AbstractReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/**
 * This class represents the return statement AST node type.
 *
 * ReturnStatement:
 * return [ Expression ] ;
 */
public class JReturnStatement extends AbstractReturnStatement implements JAstNode {

  public JReturnStatement(FileLocation pFileLocation, Optional<JExpression> pExpression) {
    // TODO We absolutely need a correct assignment here that assigns pExpression to a special variable with the return type of the function.
    super(pFileLocation, pExpression, Optional.<JAssignment>absent());

  }

  @SuppressWarnings("unchecked") // safe because Optional is covariant
  @Override
  public Optional<JExpression> getReturnValue() {
    return (Optional<JExpression>) super.getReturnValue();
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

    if (!(obj instanceof JReturnStatement)) {
      return false;
    }

    return super.equals(obj);
  }
}
