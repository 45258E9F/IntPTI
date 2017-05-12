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
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

import java.util.Objects;

/**
 * This class represents an expression unique to the java cfa.
 * It evaluates to true, if the run time type of the expression is the same
 * as the type Definition. Otherwise, it evaluates to false.
 */
public class JRunTimeTypeEqualsType extends AbstractExpression implements JExpression {

  private final JRunTimeTypeExpression runTimeTypeExpression;
  private final JClassOrInterfaceType typeDef;

  public JRunTimeTypeEqualsType(
      FileLocation pFileLocation,
      JRunTimeTypeExpression pRunTimeTypeExpression, JClassOrInterfaceType pTypeDef) {
    super(pFileLocation, JSimpleType.getBoolean());

    runTimeTypeExpression = pRunTimeTypeExpression;
    typeDef = pTypeDef;

    assert getRunTimeTypeExpression() != null;
    assert getTypeDef() != null;
  }

  @Override
  public JType getExpressionType() {
    return (JType) super.getExpressionType();
  }

  @Override
  public <R, X extends Exception> R accept(JRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString() {
    StringBuilder astString = new StringBuilder("(");
    astString.append(getRunTimeTypeExpression().toASTString());
    astString.append("_equals(");
    astString.append(
        getTypeDef().getName()); // FIXME _class missing? I.e. var_getClass()_equals(typeDef_class)?
    astString.append("))");
    return astString.toString();
  }

  public JClassOrInterfaceType getTypeDef() {
    return typeDef;
  }

  public JRunTimeTypeExpression getRunTimeTypeExpression() {
    return runTimeTypeExpression;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(runTimeTypeExpression);
    result = prime * result + Objects.hashCode(typeDef);
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

    if (!(obj instanceof JRunTimeTypeEqualsType)
        || !super.equals(obj)) {
      return false;
    }

    JRunTimeTypeEqualsType other = (JRunTimeTypeEqualsType) obj;

    return Objects.equals(other.runTimeTypeExpression, runTimeTypeExpression)
        && Objects.equals(other.typeDef, typeDef);
  }

}
