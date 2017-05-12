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
package org.sosy_lab.cpachecker.cpa.livevar;

import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACastExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.AFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayCreationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayLengthExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JEnumConstantExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JRunTimeTypeEqualsType;
import org.sosy_lab.cpachecker.cfa.ast.java.JThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableRunTimeType;

import java.util.Collections;
import java.util.Set;

/**
 * This visitor collects all ASimpleDeclarations from a given expression. This
 * is independent of the programming language of the evaluated expression.
 */
public class DeclarationCollectingVisitor
    extends AExpressionVisitor<Set<ASimpleDeclaration>, RuntimeException> {

  @Override
  public Set<ASimpleDeclaration> visit(CTypeIdExpression exp) {
    return Collections.emptySet();
  }

  @Override
  public Set<ASimpleDeclaration> visit(CImaginaryLiteralExpression exp) {
    return Collections.emptySet();
  }

  @Override
  public Set<ASimpleDeclaration> visit(CFieldReference exp) {
    return exp.getFieldOwner().accept(this);
  }

  @Override
  public Set<ASimpleDeclaration> visit(CPointerExpression exp) {
    return exp.getOperand().accept(this);
  }

  @Override
  public Set<ASimpleDeclaration> visit(CComplexCastExpression exp) {
    return exp.getOperand().accept(this);
  }

  @Override
  public Set<ASimpleDeclaration> visit(CAddressOfLabelExpression exp) {
    return Collections.emptySet();
  }

  @Override
  public Set<ASimpleDeclaration> visit(JBooleanLiteralExpression exp) {
    return Collections.emptySet();
  }

  @Override
  public Set<ASimpleDeclaration> visit(JArrayCreationExpression exp) {
    if (exp.getInitializer() != null) {
      return exp.getInitializer().accept(this);
    } else {
      return Collections.emptySet();
    }
  }

  @Override
  public Set<ASimpleDeclaration> visit(JArrayInitializer exp) {
    Set<ASimpleDeclaration> result = Collections.emptySet();
    for (JExpression innerExp : exp.getInitializerExpressions()) {
      result = Sets.union(result, innerExp.accept(this));
    }
    return result;
  }

  @Override
  public Set<ASimpleDeclaration> visit(JArrayLengthExpression exp) {
    return exp.getQualifier().accept(this);
  }

  @Override
  public Set<ASimpleDeclaration> visit(JVariableRunTimeType exp) {
    return exp.getReferencedVariable().accept(this);
  }

  @Override
  public Set<ASimpleDeclaration> visit(JRunTimeTypeEqualsType exp) {
    return exp.getRunTimeTypeExpression().accept(this);
  }

  @Override
  public Set<ASimpleDeclaration> visit(JNullLiteralExpression exp) {
    return Collections.emptySet();
  }

  @Override
  public Set<ASimpleDeclaration> visit(JEnumConstantExpression exp) {
    return Collections.emptySet();
  }

  @Override
  public Set<ASimpleDeclaration> visit(JThisExpression exp) {
    return Collections.emptySet();
  }

  @Override
  public Set<ASimpleDeclaration> visit(AArraySubscriptExpression exp) {
    return Sets.union(accept0(exp.getArrayExpression()),
        accept0(exp.getSubscriptExpression()));
  }

  @Override
  public Set<ASimpleDeclaration> visit(AIdExpression exp) {
    return Collections.singleton(exp.getDeclaration());
  }

  @Override
  public Set<ASimpleDeclaration> visit(ABinaryExpression exp) {
    return Sets.union(accept0(exp.getOperand1()),
        accept0(exp.getOperand2()));
  }

  @Override
  public Set<ASimpleDeclaration> visit(ACastExpression exp) {
    return accept0(exp.getOperand());
  }

  @Override
  public Set<ASimpleDeclaration> visit(ACharLiteralExpression exp) {
    return Collections.emptySet();
  }

  @Override
  public Set<ASimpleDeclaration> visit(AFloatLiteralExpression exp) {
    return Collections.emptySet();
  }

  @Override
  public Set<ASimpleDeclaration> visit(AIntegerLiteralExpression exp) {
    return Collections.emptySet();
  }

  @Override
  public Set<ASimpleDeclaration> visit(AStringLiteralExpression exp) {
    return Collections.emptySet();
  }

  @Override
  public Set<ASimpleDeclaration> visit(AUnaryExpression exp) {
    return accept0(exp.getOperand());
  }

  private Set<ASimpleDeclaration> accept0(AExpression exp) {
    return exp.<Set<ASimpleDeclaration>,
        Set<ASimpleDeclaration>,
        Set<ASimpleDeclaration>,
        RuntimeException,
        RuntimeException,
        DeclarationCollectingVisitor>accept_(this);
  }
}
