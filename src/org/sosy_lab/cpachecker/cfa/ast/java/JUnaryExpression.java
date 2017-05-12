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

import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * This class represents the prefix expression AST node type.
 *
 * PrefixExpression:
 * PrefixOperator Expression
 *
 * This class does not represent increments or decrements, they are transformed
 * to {@link JBinaryExpression}.
 */
public class JUnaryExpression extends AUnaryExpression implements JExpression {

  public JUnaryExpression(
      FileLocation pFileLocation,
      JType pType,
      JExpression pOperand,
      UnaryOperator pOperator) {
    super(pFileLocation, pType, pOperand, pOperator);

  }

  @Override
  public JType getExpressionType() {
    return (JType) super.getExpressionType();
  }

  @Override
  public JExpression getOperand() {
    return (JExpression) super.getOperand();
  }

  @Override
  public UnaryOperator getOperator() {
    return (UnaryOperator) super.getOperator();
  }

  @Override
  public <R, X extends Exception> R accept(JRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  public static enum UnaryOperator implements AUnaryExpression.AUnaryOperator {


    NOT("!"),
    PLUS("+"),
    COMPLEMENT("~"),
    MINUS("-");

    private final String op;

    private UnaryOperator(String pOp) {
      op = pOp;
    }

    /**
     * Returns the string representation of this operator (e.g. "*", "+").
     */
    @Override
    public String getOperator() {
      return op;
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

    if (!(obj instanceof JUnaryExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}
