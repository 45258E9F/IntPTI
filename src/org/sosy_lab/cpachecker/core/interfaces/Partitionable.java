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
package org.sosy_lab.cpachecker.core.interfaces;

import javax.annotation.Nullable;

/**
 * Interface for classes that allow their objects to be partitioned.
 */
public interface Partitionable {

  /**
   * Returns the key of the current object that indicates in which part of the
   * partition the object belongs.
   *
   * The result of this method:
   * - may be null
   * - must provide meaningful and consistent equals() and hashCode() implementations
   * - must stay constant regarding the relation defined by equals() throughout
   * the whole lifetime of this object
   *
   * - needs not to stay constant regarding the object identity relation
   * defined by the == operator
   * - should probably never be an array (they miss a meaningful equals() implementation),
   * use an list instead
   *
   * There is no restriction on the number of different (regarding equals())
   * keys the objects of one class may have: It is legal for all objects to
   * have the same key as well as for all objects to have different keys.
   * In the former case this method might always return null.
   * In the latter case this method might for example return the current object
   * or the value of {@link Object#hashCode()} for the object (which may be
   * obtained by {@link System#identityHashCode(Object)} even if a superclass
   * overwrites hashCode()).
   *
   * @return a key indicating the part of the partition this object belongs to
   */
  @Nullable
  Object getPartitionKey();

}
