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
package org.sosy_lab.cpachecker.util.predicates.regions;

/**
 * An AbstractFormula is a representation of a data region in the
 * abstract space. For instance, in the case of predicate abstraction, it can
 * be a BDD over the predicates
 */
public interface Region {

  /**
   * checks whether f represents "true"
   *
   * @return true if f represents logical truth, false otherwise
   */
  boolean isTrue();

  /**
   * checks whether f represents "false"
   *
   * @return true if f represents logical falsity, false otherwise
   */
  boolean isFalse();
}
