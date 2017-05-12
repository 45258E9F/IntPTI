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
package org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class CachingRelevantPredicatesComputer implements RefineableRelevantPredicatesComputer {

  private final Map<Pair<Block, ImmutableSet<AbstractionPredicate>>, ImmutableSet<AbstractionPredicate>>
      irrelevantCache = Maps.newHashMap();
  private final Map<Pair<Block, ImmutableSet<AbstractionPredicate>>, ImmutableSet<AbstractionPredicate>>
      relevantCache = Maps.newHashMap();

  private final RelevantPredicatesComputer delegate;

  public CachingRelevantPredicatesComputer(RelevantPredicatesComputer pDelegate) {
    delegate = checkNotNull(pDelegate);
  }

  @Override
  public Set<AbstractionPredicate> getIrrelevantPredicates(
      Block pContext, Collection<AbstractionPredicate> pPredicates) {

    if (pPredicates.isEmpty()) {
      return Collections.emptySet();
    }

    ImmutableSet<AbstractionPredicate> predicates = ImmutableSet.copyOf(pPredicates);
    Pair<Block, ImmutableSet<AbstractionPredicate>> key = Pair.of(pContext, predicates);

    ImmutableSet<AbstractionPredicate> result = irrelevantCache.get(key);
    if (result == null) {
      result = ImmutableSet.copyOf(delegate.getIrrelevantPredicates(pContext, predicates));
      irrelevantCache.put(key, result);
    }

    return result;
  }

  @Override
  public Set<AbstractionPredicate> getRelevantPredicates(
      Block pContext,
      Collection<AbstractionPredicate> pPredicates) {

    if (pPredicates.isEmpty()) {
      return Collections.emptySet();
    }

    ImmutableSet<AbstractionPredicate> predicates = ImmutableSet.copyOf(pPredicates);
    Pair<Block, ImmutableSet<AbstractionPredicate>> key = Pair.of(pContext, predicates);

    ImmutableSet<AbstractionPredicate> result = relevantCache.get(key);
    if (result == null) {
      result = ImmutableSet.copyOf(delegate.getRelevantPredicates(pContext, predicates));
      relevantCache.put(key, result);
    }

    return result;
  }

  @Override
  public CachingRelevantPredicatesComputer considerPredicatesAsRelevant(
      Block pBlock,
      Set<AbstractionPredicate> pPredicates) {
    if (delegate instanceof RefineableRelevantPredicatesComputer) {
      RefineableRelevantPredicatesComputer refineableDelegate =
          (RefineableRelevantPredicatesComputer) delegate;
      RefineableRelevantPredicatesComputer newComputer =
          refineableDelegate.considerPredicatesAsRelevant(pBlock, pPredicates);

      if (newComputer == refineableDelegate) {
        return this;
      }

      CachingRelevantPredicatesComputer newCachingComputer =
          new CachingRelevantPredicatesComputer(newComputer);

      // we copy every useful data into the new Computer
      putAllExceptBlock(this.relevantCache, newCachingComputer.relevantCache, pBlock);
      putAllExceptBlock(this.irrelevantCache, newCachingComputer.irrelevantCache, pBlock);

      return newCachingComputer;
    }
    return this;
  }

  private static <U, V> void putAllExceptBlock(
      Map<Pair<Block, U>, V> from,
      Map<Pair<Block, U>, V> to,
      Block block) {
    for (Entry<Pair<Block, U>, V> entry : from.entrySet()) {
      if (!block.equals(entry.getKey().getFirst())) {
        to.put(entry.getKey(), entry.getValue());
      }
    }
  }

  @Override
  public String toString() {
    return "CachingRelevantPredicatesComputer (" + delegate + ")";
  }
}
