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

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.Type;

/**
 * Abstract class for side-effect free expressions.
 * This class is only SuperClass of all abstract Classes and their Subclasses.
 * The Interface {@link AExpression} contains all language specific
 * AST Nodes as well.
 */
public abstract class AbstractExpression extends AbstractRightHandSide implements AExpression {

  public AbstractExpression(FileLocation pFileLocation, Type pType) {
    super(pFileLocation, pType);
  }

  @Override
  public final <R, R1 extends R, R2 extends R,
      X1 extends Exception, X2 extends Exception,
      V extends CExpressionVisitor<R1, X1> & JExpressionVisitor<R2, X2>>
  R accept_(V v) throws X1, X2 {
    if (this instanceof CExpression) {
      return ((CExpression) this).accept(v);
    } else if (this instanceof JExpression) {
      return ((JExpression) this).accept(v);
    } else {
      throw new AssertionError(
          "AbstractExpression.accept_ needs to be extended for new languages.");
    }
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

    if (!(obj instanceof AbstractExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}
