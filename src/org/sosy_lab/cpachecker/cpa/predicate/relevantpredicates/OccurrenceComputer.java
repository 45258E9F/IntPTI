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
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

import java.util.Collection;


/**
 * Computes set of irrelevant predicates of a block by identifying the variables that do not occur
 * in the block.
 */

public class OccurrenceComputer extends AbstractRelevantPredicatesComputer<Block> {

  public OccurrenceComputer(FormulaManagerView pFmgr) {
    super(pFmgr);
  }

  @Override
  protected Block precompute(Block pContext, Collection<AbstractionPredicate> pPredicates) {
    return pContext;
  }

  @Override
  protected boolean isRelevant(Block context, AbstractionPredicate predicate) {
    String predicateString = predicate.getSymbolicAtom().toString();

    for (ReferencedVariable var : context.getReferencedVariables()) {
      if (predicateString.contains(var.getName())) {
        //var occurs in the predicate, so better trace it
        //TODO: contains is a quite rough approximation; for example "foo <= 5" also contains "f", although the variable f does in fact not occur in the predicate.
        return true;
      }
    }
    return false;
  }
}