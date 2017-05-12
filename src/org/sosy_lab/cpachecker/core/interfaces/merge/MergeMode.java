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
package org.sosy_lab.cpachecker.core.interfaces.merge;

/**
 * Merge mode supported by hybrid merge operator.
 */
public enum MergeMode {

  /**
   * For two abstract states s1 and s2 corresponding to the same location, "SEP" mode computes
   * successors of s1 and s2 respectively on the following.
   */
  SEP,
  /**
   * For two abstract states s1 and s2 corresponding to the same location, "JOIN" mode merges
   * them into a new state s0 and then compute successors for s0. The execution paths starting
   * from s1 and s2 are merged into a path starting from s0.
   */
  JOIN

}
