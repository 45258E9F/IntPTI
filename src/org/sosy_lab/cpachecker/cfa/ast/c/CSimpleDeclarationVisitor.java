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

import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;

public interface CSimpleDeclarationVisitor<R, X extends Exception> {

  R visit(CFunctionDeclaration pDecl) throws X;

  R visit(CComplexTypeDeclaration pDecl) throws X;

  R visit(CTypeDeclaration pDecl) throws X;

  R visit(CVariableDeclaration pDecl) throws X;

  R visit(CParameterDeclaration pDecl) throws X;

  R visit(CEnumerator pDecl) throws X;

}
