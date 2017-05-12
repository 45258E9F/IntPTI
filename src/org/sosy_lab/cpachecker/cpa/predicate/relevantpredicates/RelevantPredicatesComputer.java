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
package org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import java.util.Collection;
import java.util.Set;


/**
 * Interface for the computation of (ir-)relevant predicates of a given block.
 *
 * We assume, that a computer is immutable. We allow internal caching and optimizations,
 * but the set of (ir-)relevant variables must remain constant (within one instance).
 */
public interface RelevantPredicatesComputer {
  public Set<AbstractionPredicate> getIrrelevantPredicates(
      Block context,
      Collection<AbstractionPredicate> predicates);

  public Set<AbstractionPredicate> getRelevantPredicates(
      Block context,
      Collection<AbstractionPredicate> predicates);
}
