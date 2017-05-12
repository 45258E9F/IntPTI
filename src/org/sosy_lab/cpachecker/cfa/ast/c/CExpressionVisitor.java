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
package org.sosy_lab.cpachecker.cfa.ast.c;


public interface CExpressionVisitor<R, X extends Exception> extends CLeftHandSideVisitor<R, X> {

  R visit(CBinaryExpression pIastBinaryExpression) throws X;

  R visit(CCastExpression pIastCastExpression) throws X;

  R visit(CCharLiteralExpression pIastCharLiteralExpression) throws X;

  R visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws X;

  R visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) throws X;

  R visit(CStringLiteralExpression pIastStringLiteralExpression) throws X;

  R visit(CTypeIdExpression pIastTypeIdExpression) throws X;

  R visit(CUnaryExpression pIastUnaryExpression) throws X;

  R visit(CImaginaryLiteralExpression PIastLiteralExpression) throws X;

  R visit(CAddressOfLabelExpression pAddressOfLabelExpression) throws X;
}
