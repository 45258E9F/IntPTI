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
package org.sosy_lab.cpachecker.core.interfaces.merge;

/**
 * The enumeration of currently supported merge strategy.
 */
public enum MergeStrategyIdentifier {

  /**
   * Merge states based on similarity of control flow until their first confluence point.
   */
  CLONE_CODE("MergeCloneCode"),
  /**
   * Merge states by abstracting variables with different values. Generally the granularity of
   * abstraction is controlled by a parameter <code>k</code>.
   */
  ABSTRACT_DIFF_VAR("MergeDifferentVar");

  private static final String SUFFIX = "Tactic";
  private final String className;

  MergeStrategyIdentifier(String pClassName) {
    className = pClassName.concat(SUFFIX);
  }

  public String getClassName() {
    return className;
  }

}
