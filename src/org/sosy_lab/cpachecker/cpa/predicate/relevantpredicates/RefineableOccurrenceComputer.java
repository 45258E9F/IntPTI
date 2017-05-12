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

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSetMultimap.Builder;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

import java.util.Set;


/**
 * Computes set of irrelevant predicates of a block by identifying the variables that do not occur
 * in the block.
 */

public class RefineableOccurrenceComputer extends OccurrenceComputer
    implements RefineableRelevantPredicatesComputer {

  private final ImmutableSetMultimap<Block, AbstractionPredicate> definitelyRelevantPredicates;

  public RefineableOccurrenceComputer(FormulaManagerView pFmgr) {
    super(pFmgr);
    definitelyRelevantPredicates = ImmutableSetMultimap.of();
  }

  private RefineableOccurrenceComputer(
      FormulaManagerView pFmgr,
      ImmutableSetMultimap<Block, AbstractionPredicate> pDefinitelyRelevantPredicates) {
    super(pFmgr);
    definitelyRelevantPredicates = pDefinitelyRelevantPredicates;
  }

  @Override
  protected boolean isRelevant(Block context, AbstractionPredicate predicate) {
    Set<AbstractionPredicate> relevantPredicates = definitelyRelevantPredicates.get(context);
    if (relevantPredicates != null && relevantPredicates.contains(predicate)) {
      return true;
    }

    return super.isRelevant(context, predicate);
  }

  @Override
  public RefineableOccurrenceComputer considerPredicatesAsRelevant(
      Block block, Set<AbstractionPredicate> predicates) {

    Set<AbstractionPredicate> newPreds =
        Sets.difference(predicates, definitelyRelevantPredicates.get(block));

    if (newPreds.isEmpty()) {
      return this;
    }

    Builder<Block, AbstractionPredicate> builder = ImmutableSetMultimap.builder();
    builder.putAll(definitelyRelevantPredicates);
    builder.putAll(block, newPreds);
    return new RefineableOccurrenceComputer(fmgr, builder.build());
  }

  @Override
  public String toString() {
    return "RefineableOccurrenceComputer (" + definitelyRelevantPredicates + ")";
  }
}