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

import org.sosy_lab.cpachecker.cfa.types.Type;

import javax.annotation.Nullable;

public interface CType extends Type {

  public boolean isConst();

  @Override
  public abstract String toString();

  public boolean isVolatile();

  /**
   * Will throw a UnsupportedOperationException
   */
  @Override
  public int hashCode();

  /**
   * Be careful, this method compares the CType as it is to the given object,
   * typedefs won't be resolved. If you want to compare the type without having
   * typedefs in it use #getCanonicalType().equals()
   */
  @Override
  public boolean equals(@Nullable Object obj);

  public abstract <R, X extends Exception> R accept(CTypeVisitor<R, X> visitor) throws X;

  public CType getCanonicalType();

  public CType getCanonicalType(boolean forceConst, boolean forceVolatile);
}
