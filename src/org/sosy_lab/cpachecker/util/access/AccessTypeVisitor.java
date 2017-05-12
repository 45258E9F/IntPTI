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
package org.sosy_lab.cpachecker.util.access;

import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

import java.util.ArrayList;
import java.util.List;

/**
 * get the type String
 * 1. remove typedef
 * 2. remove function relevant
 *
 * struct s[] -> s.struct.pointer
 * int[] -> int.pointer
 * struct *s -> s.struct.pointer
 * int -> simple
 */
public class AccessTypeVisitor implements CTypeVisitor<List<String>, UnrecognizedCCodeException> {
  private static AccessTypeVisitor instance;

  private AccessTypeVisitor() {
  }

  public static AccessTypeVisitor getInstance() {
    if (instance == null) {
      instance = new AccessTypeVisitor();
    }
    return instance;
  }

  @Override
  public List<String> visit(CArrayType pArrayType) throws UnrecognizedCCodeException {
    List<String> result = pArrayType.getType().accept(this);
    result.add(TypeKind.POINTER.toString());
    return result;
  }

  @Override
  public List<String> visit(CCompositeType pCompositeType) throws UnrecognizedCCodeException {
    List<String> result = new ArrayList<>();
    result.add(pCompositeType.getName());
    result.add(pCompositeType.getKind().toASTString());
    return result;
  }

  @Override
  public List<String> visit(CElaboratedType pElaboratedType) throws UnrecognizedCCodeException {
    List<String> result = new ArrayList<>();
    if (pElaboratedType.getRealType() == null) {
      result.add(TypeKind.ANYNAME.toASTString());
      result.add(pElaboratedType.getKind().toASTString());
      return result;
    }
    result = pElaboratedType.getRealType().accept(this);
    return result;
  }

  @Override
  public List<String> visit(CEnumType pEnumType) throws UnrecognizedCCodeException {
    List<String> result = new ArrayList<>();
    // we save the getEnumerators()
    result = pEnumType.getEnumerators().get(0).getType().accept(this);
    return result;
  }

  @Override
  public List<String> visit(CFunctionType pFunctionType) throws UnrecognizedCCodeException {
    List<String> result = new ArrayList<>();
    result.add(TypeKind.FUNCTION.toASTString());
    return result;
  }

  @Override
  public List<String> visit(CPointerType pPointerType) throws UnrecognizedCCodeException {
    List<String> result = pPointerType.getType().accept(this);
    result.add(TypeKind.POINTER.toString());
    return result;
  }

  @Override
  public List<String> visit(CProblemType pProblemType) throws UnrecognizedCCodeException {
    List<String> result = new ArrayList<>();
    return result;
  }

  @Override
  public List<String> visit(CSimpleType pSimpleType) throws UnrecognizedCCodeException {
    List<String> result = new ArrayList<>();
    result.add(TypeKind.SIMPLE.toString());
    return result;
  }

  @Override
  public List<String> visit(CTypedefType pTypedefType) throws UnrecognizedCCodeException {
    List<String> result = pTypedefType.getRealType().accept(this);
    return result;
  }

  @Override
  public List<String> visit(CVoidType pVoidType) throws UnrecognizedCCodeException {
    List<String> result = new ArrayList<>();
    result.add(TypeKind.VOID.toString());
    return result;
  }

  public static enum TypeKind {
    POINTER,
    SIMPLE,
    ANYNAME,
    // this is for CElaboratedType without realtype
    FUNCTION,
    VOID;

    public String toASTString() {
      return name().toUpperCase();
    }
  }

}
