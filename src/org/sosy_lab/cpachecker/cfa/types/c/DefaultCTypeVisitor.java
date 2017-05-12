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
package org.sosy_lab.cpachecker.cfa.types.c;


public abstract class DefaultCTypeVisitor<R, X extends Exception>
    implements CTypeVisitor<R, X> {

  public abstract R visitDefault(final CType t) throws X;

  @Override
  public R visit(final CArrayType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CCompositeType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CElaboratedType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CEnumType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CFunctionType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CPointerType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CProblemType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CSimpleType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CTypedefType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CVoidType t) throws X {
    return visitDefault(t);
  }
}

