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

import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;

import java.util.List;
import java.util.Objects;

/**
 * Array creation expression AST node type.
 *
 * ArrayCreation:
 * new PrimitiveType [ Expression ] { [ Expression ] } { [ ] }
 * new TypeName [ < Type { , Type } > ]
 * [ Expression ] { [ Expression ] } { [ ] }
 * new PrimitiveType [ ] { [ ] } ArrayInitializer
 * new TypeName [ < Type { , Type } > ]
 * [ ] { [ ] } ArrayInitializer
 *
 *
 * The mapping from Java language syntax to AST nodes is as follows:
 *
 * the type node is the array type of the creation expression. It contains information
 * like the dimension and the element type.
 * The length contains the expression, which determines the length of the array.
 * There is an expression in the list for each array dimension from left to right.
 */
public class JArrayCreationExpression extends AbstractExpression implements JExpression {

  private final List<JExpression> length;
  private final JArrayInitializer initializer;
  //TODO Type Variables < Type { , Type } >

  public JArrayCreationExpression(
      FileLocation pFileLocation,
      JArrayType pType,
      JArrayInitializer pInitializer,
      List<JExpression> pLength) {
    super(pFileLocation, pType);
    length = ImmutableList.copyOf(pLength);
    initializer = pInitializer;

  }

  @Override
  public JArrayType getExpressionType() {
    return (JArrayType) super.getExpressionType();
  }

  @Override
  public <R, X extends Exception> R accept(JRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString() {
    if (initializer != null) {
      return initializer.toASTString();
    } else {

      StringBuilder astString =
          new StringBuilder("new " + getExpressionType().getElementType().toASTString(""));

      for (JExpression exp : length) {
        astString.append("[");
        astString.append(exp.toASTString());
        astString.append("]");
      }

      return astString.toString();
    }
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  public List<JExpression> getLength() {
    return length;
  }

  public JArrayInitializer getInitializer() {
    return initializer;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(initializer);
    result = prime * result + Objects.hashCode(length);
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

    if (!(obj instanceof JArrayCreationExpression)
        || !super.equals(obj)) {
      return false;
    }

    JArrayCreationExpression other = (JArrayCreationExpression) obj;

    return Objects.equals(other.initializer, initializer)
        && Objects.equals(other.length, length);
  }

}
