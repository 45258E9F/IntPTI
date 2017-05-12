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

public interface AbstractState {

  /**
   * Many abstract states use Object.equals() to examine the equality of two states. However in
   * many cases we need a weaker equality checking method. Given two different state instance,
   * their contents can be equal.
   */
  boolean isEqualTo(AbstractState other);

}
