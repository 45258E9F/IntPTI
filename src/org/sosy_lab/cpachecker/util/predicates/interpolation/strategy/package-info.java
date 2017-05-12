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

/**
 * Different strategies to compute interpolants for a sequence of path formulae.
 * Some analyses depend on special strategies, e.g.
 * predicate analysis needs an inductive sequence of interpolation and
 * the analysis of recursive procedures with BAM and predicate analysis uses tree interpolation.
 * There are also strategies that directly ask the SMT solver for a complete solution
 * instead of querying every interpolant separately.
 */
package org.sosy_lab.cpachecker.util.predicates.interpolation.strategy;
