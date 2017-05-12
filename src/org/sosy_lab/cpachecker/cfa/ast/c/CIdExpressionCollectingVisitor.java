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

import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Set;


public class CIdExpressionCollectingVisitor
    extends DefaultCExpressionVisitor<Set<CIdExpression>, RuntimeException>
    implements CStatementVisitor<Set<CIdExpression>, RuntimeException>,
               CRightHandSideVisitor<Set<CIdExpression>, RuntimeException>,
               CInitializerVisitor<Set<CIdExpression>, RuntimeException>,
               CDesignatorVisitor<Set<CIdExpression>, RuntimeException> {

  @Override
  protected Set<CIdExpression> visitDefault(CExpression pExp) {
    return Collections.emptySet();
  }

  @Override
  public Set<CIdExpression> visit(CArraySubscriptExpression pE) {
    return Sets.union(
        pE.getArrayExpression().accept(this),
        pE.getSubscriptExpression().accept(this));
  }

  @Override
  public Set<CIdExpression> visit(CBinaryExpression pE) {
    return Sets.union(
        pE.getOperand1().accept(this),
        pE.getOperand2().accept(this));
  }

  @Override
  public Set<CIdExpression> visit(CCastExpression pE) {
    return pE.getOperand().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CComplexCastExpression pE) {
    return pE.getOperand().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CFieldReference pE) {
    return pE.getFieldOwner().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CPointerExpression pE) {
    return pE.getOperand().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CUnaryExpression pE) {
    return pE.getOperand().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CInitializerExpression pE) {
    return pE.getExpression().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CInitializerList pI) {
    Set<CIdExpression> result = Collections.emptySet();
    for (CInitializer i : pI.getInitializers()) {
      result = Sets.union(result, i.accept(this));
    }
    return result;
  }

  @Override
  public Set<CIdExpression> visit(CDesignatedInitializer pI) {
    Set<CIdExpression> result = Collections.emptySet();
    for (CDesignator d : pI.getDesignators()) {
      result = Sets.union(result, d.accept(this));
    }
    if (pI.getRightHandSide() != null) {
      result = Sets.union(result, pI.getRightHandSide().accept(this));
    }
    return result;
  }

  @Override
  public Set<CIdExpression> visit(CExpressionAssignmentStatement pS) {
    return Sets.union(
        pS.getLeftHandSide().accept(this),
        pS.getRightHandSide().accept(this));
  }

  @Override
  public Set<CIdExpression> visit(CExpressionStatement pS) {
    return pS.getExpression().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CFunctionCallAssignmentStatement pS) {
    Set<CIdExpression> result = Sets.union(
        pS.getLeftHandSide().accept(this),
        pS.getRightHandSide().getFunctionNameExpression().accept(this));

    for (CExpression expr : pS.getRightHandSide().getParameterExpressions()) {
      result = Sets.union(result, expr.accept(this));
    }

    return result;
  }

  @Override
  public Set<CIdExpression> visit(CFunctionCallStatement pS) {
    return pS.getFunctionCallExpression().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CIdExpression pE) {
    return Collections.singleton(pE);
  }

  @Override
  public Set<CIdExpression> visit(CArrayDesignator pArrayDesignator) {
    return pArrayDesignator.getSubscriptExpression().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CArrayRangeDesignator pArrayRangeDesignator) {
    return Sets.union(
        pArrayRangeDesignator.getFloorExpression().accept(this),
        pArrayRangeDesignator.getCeilExpression().accept(this));
  }

  @Override
  public Set<CIdExpression> visit(CFieldDesignator pFieldDesignator) {
    return Collections.emptySet();
  }

  @Override
  public Set<CIdExpression> visit(CFunctionCallExpression pIastFunctionCallExpression) {
    Set<CIdExpression> result = Collections.emptySet();
    for (CExpression e : pIastFunctionCallExpression.getParameterExpressions()) {
      result = Sets.union(result, e.accept(this));
    }
    return result;
  }
}
