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
package org.sosy_lab.cpachecker.cpa.shape.merge.abs;

/**
 * The general interface for value abstraction pair.
 */
public interface AbstractionPair<T extends AbstractValue> {

  T getLeft();

  T getRight();

  Long getLeftValue();

  Long getRightValue();

  void updateLeftValue(Long v);

  void updateRightValue(Long v);

  @Override
  boolean equals(Object obj);

}
