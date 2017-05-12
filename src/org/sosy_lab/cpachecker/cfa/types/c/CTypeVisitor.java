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
package org.sosy_lab.cpachecker.cfa.types.c;

public interface CTypeVisitor<R, X extends Exception> {
  R visit(CArrayType pArrayType) throws X;

  R visit(CCompositeType pCompositeType) throws X;

  R visit(CElaboratedType pElaboratedType) throws X;

  R visit(CEnumType pEnumType) throws X;

  R visit(CFunctionType pFunctionType) throws X;

  R visit(CPointerType pPointerType) throws X;

  R visit(CProblemType pProblemType) throws X;

  R visit(CSimpleType pSimpleType) throws X;

  R visit(CTypedefType pTypedefType) throws X;

  R visit(CVoidType pVoidType) throws X;
}
