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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;


class IsRelevantLhsVisitor extends DefaultCExpressionVisitor<Boolean, RuntimeException> {

  private final CtoFormulaConverter conv;

  IsRelevantLhsVisitor(CtoFormulaConverter pConv) {
    conv = pConv;
  }

  @Override
  public Boolean visit(final CArraySubscriptExpression e) {
    return e.getArrayExpression().accept(this);
  }

  @Override
  public Boolean visit(final CCastExpression e) {
    return e.getOperand().accept(this);
  }

  @Override
  public Boolean visit(final CComplexCastExpression e) {
    return e.getOperand().accept(this);
  }

  @Override
  public Boolean visit(final CFieldReference e) {
    CType fieldOwnerType = e.getFieldOwner().getExpressionType().getCanonicalType();
    if (fieldOwnerType instanceof CPointerType) {
      fieldOwnerType = ((CPointerType) fieldOwnerType).getType();
    }
    assert fieldOwnerType instanceof CCompositeType : "Field owner should have composite type";
    return conv.isRelevantField((CCompositeType) fieldOwnerType, e.getFieldName());
  }

  @Override
  public Boolean visit(final CIdExpression e) {
    return conv.isRelevantVariable(e.getDeclaration());
  }

  @Override
  public Boolean visit(CPointerExpression e) {
    return true;
  }

  @Override
  protected Boolean visitDefault(CExpression e) {
    throw new IllegalArgumentException("Undexpected left hand side: " + e.toString());
  }
}
