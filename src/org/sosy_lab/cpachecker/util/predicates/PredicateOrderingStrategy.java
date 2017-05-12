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
package org.sosy_lab.cpachecker.util.predicates;

/**
 * This enum represents the different strategies available for sorting the bdd variables that store
 * predicates during the predicate analysis.
 */
public enum PredicateOrderingStrategy {
  SIMILARITY,
  FREQUENCY,
  IMPLICATION,
  REV_IMPLICATION,
  RANDOMLY,
  FRAMEWORK_RANDOM,
  FRAMEWORK_SIFT,
  FRAMEWORK_SIFTITE,
  FRAMEWORK_WIN2,
  FRAMEWORK_WIN2ITE,
  FRAMEWORK_WIN3,
  FRAMEWORK_WIN3ITE,
  CHRONOLOGICAL;

  private boolean isFrameworkStrategy;

  static {
    SIMILARITY.isFrameworkStrategy = false;
    FREQUENCY.isFrameworkStrategy = false;
    IMPLICATION.isFrameworkStrategy = false;
    REV_IMPLICATION.isFrameworkStrategy = false;
    RANDOMLY.isFrameworkStrategy = false;
    FRAMEWORK_RANDOM.isFrameworkStrategy = true;
    FRAMEWORK_SIFT.isFrameworkStrategy = true;
    FRAMEWORK_SIFTITE.isFrameworkStrategy = true;
    FRAMEWORK_WIN2.isFrameworkStrategy = true;
    FRAMEWORK_WIN2ITE.isFrameworkStrategy = true;
    FRAMEWORK_WIN3.isFrameworkStrategy = true;
    FRAMEWORK_WIN3ITE.isFrameworkStrategy = true;
    CHRONOLOGICAL.isFrameworkStrategy = true;
  }

  public boolean getIsFrameworkStrategy() {
    return this.isFrameworkStrategy;
  }
}
