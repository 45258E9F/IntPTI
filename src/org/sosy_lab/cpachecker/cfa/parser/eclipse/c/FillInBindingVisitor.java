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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.DefaultCTypeVisitor;

/**
 * Visitor that fills in missing bindings of CElaboratedTypes with a given
 * target type (if name and kind match, of course).
 */
class FillInBindingVisitor extends DefaultCTypeVisitor<Void, RuntimeException> {

  private final ComplexTypeKind kind;
  private final String name;
  private final CComplexType target;

  FillInBindingVisitor(ComplexTypeKind pKind, String pName, CComplexType pTarget) {
    kind = pKind;
    name = pName;
    target = pTarget;
  }

  @Override
  public Void visitDefault(CType pT) {
    return null;
  }

  @Override
  public Void visit(CArrayType pArrayType) {
    pArrayType.getType().accept(this);
    return null;
  }

  @Override
  public Void visit(CCompositeType pCompositeType) {
    for (CCompositeTypeMemberDeclaration member : pCompositeType.getMembers()) {
      member.getType().accept(this);
    }
    return null;
  }

  @Override
  public Void visit(CElaboratedType pElaboratedType) {
    if (pElaboratedType.getRealType() == null
        && pElaboratedType.getKind() == kind
        && pElaboratedType.getName().equals(name)) {

      pElaboratedType.setRealType(target);
    }
    return null;
  }

  @Override
  public Void visit(CFunctionType pFunctionType) {
    pFunctionType.getReturnType().accept(this);
    for (CType parameter : pFunctionType.getParameters()) {
      parameter.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(CPointerType pPointerType) {
    pPointerType.getType().accept(this);
    return null;
  }

  @Override
  public Void visit(CTypedefType pTypedefType) {
    pTypedefType.getRealType().accept(this);
    return null;
  }
}