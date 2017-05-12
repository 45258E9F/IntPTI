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

import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;

/**
 * This expression represents an array.length expression.
 *
 * <p>Example:
 * <pre>
 *    int[] a;
 *    int b = a.length;
 *  </pre>
 * </p>
 */
public abstract class JArrayLengthExpression extends AbstractExpression
    implements JExpression, JAstNode, JRightHandSide {

  private JArrayLengthExpression(FileLocation pFileLocation) {
    super(pFileLocation, JSimpleType.getInt());
  }

  public static JArrayLengthExpression getInstance(JExpression pQualifier, FileLocation pLocation) {
    if (pQualifier instanceof JArraySubscriptExpression) {
      return new JSubArrayLengthExpression(pLocation, (JArraySubscriptExpression) pQualifier);

    } else if (pQualifier instanceof JIdExpression) {
      return new JTopArrayLengthExpression(pLocation, (JIdExpression) pQualifier);

    } else {
      throw new AssertionError("Unexpected expression " + pQualifier);
    }
  }

  public abstract JExpression getQualifier();

  @Override
  public JSimpleType getExpressionType() {
    return (JSimpleType) super.getExpressionType();
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(JRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  private static class JSubArrayLengthExpression extends JArrayLengthExpression {
    private final JArraySubscriptExpression qualifier;

    public JSubArrayLengthExpression(
        FileLocation pFileLocation,
        JArraySubscriptExpression pQualifier) {
      super(pFileLocation);
      qualifier = pQualifier;
    }


    @Override
    public String toASTString() {
      return qualifier + ".length";
    }

    @Override
    public JArraySubscriptExpression getQualifier() {
      return qualifier;
    }
  }

  private static class JTopArrayLengthExpression extends JArrayLengthExpression {

    private final JIdExpression qualifier;

    private JTopArrayLengthExpression(FileLocation pFileLocation, JIdExpression pQualifier) {
      super(pFileLocation);
      qualifier = pQualifier;
    }

    @Override
    public String toASTString() {
      return qualifier + ".length";
    }

    @Override
    public JIdExpression getQualifier() {
      return qualifier;
    }
  }
}
