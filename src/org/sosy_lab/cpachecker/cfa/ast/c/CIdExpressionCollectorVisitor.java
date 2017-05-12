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
package org.sosy_lab.cpachecker.cfa.ast.c;

import java.util.HashSet;
import java.util.Set;


public class CIdExpressionCollectorVisitor
    extends DefaultCExpressionVisitor<Void, RuntimeException> {

  public static Set<String> getVariablesOfExpression(CExpression expr) {
    Set<String> result = new HashSet<>();
    CIdExpressionCollectorVisitor collector = new CIdExpressionCollectorVisitor();

    expr.accept(collector);

    for (CIdExpression id : collector.getReferencedIdExpressions()) {
      String assignToVar = id.getDeclaration().getQualifiedName();
      result.add(assignToVar);
    }

    return result;
  }

  private final Set<CIdExpression> referencedVariables = new HashSet<>();

  public Set<CIdExpression> getReferencedIdExpressions() {
    return referencedVariables;
  }

  @Override
  protected Void visitDefault(CExpression pExp) {
    return null;
  }

  @Override
  public Void visit(CIdExpression pIastIdExpression) {
    referencedVariables.add(pIastIdExpression);
    return null;
  }

  @Override
  public Void visit(CArraySubscriptExpression pIastArraySubscriptExpression) {
    pIastArraySubscriptExpression.getArrayExpression().accept(this);
    pIastArraySubscriptExpression.getSubscriptExpression().accept(this);
    return null;
  }

  @Override
  public Void visit(org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression pIastBinaryExpression) {
    pIastBinaryExpression.getOperand1().accept(this);
    pIastBinaryExpression.getOperand2().accept(this);
    return null;
  }

  @Override
  public Void visit(CCastExpression pIastCastExpression) {
    return pIastCastExpression.getOperand().accept(this);
  }

  @Override
  public Void visit(CComplexCastExpression pIastCastExpression) {
    return pIastCastExpression.getOperand().accept(this);
  }

  @Override
  public Void visit(CFieldReference pIastFieldReference) {
    return pIastFieldReference.getFieldOwner().accept(this);
  }

  @Override
  public Void visit(CUnaryExpression pIastUnaryExpression) {
    return pIastUnaryExpression.getOperand().accept(this);
  }

  @Override
  public Void visit(CPointerExpression pIastUnaryExpression) {
    return pIastUnaryExpression.getOperand().accept(this);
  }
}
