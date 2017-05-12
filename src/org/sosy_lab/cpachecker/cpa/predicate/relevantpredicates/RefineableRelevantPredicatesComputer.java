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

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import java.util.Set;


public interface RefineableRelevantPredicatesComputer extends RelevantPredicatesComputer {

  /**
   * returns a new instance of this computer,
   * where all new predicates are added as relevant predicates.
   * Returns itself, if nothing is changed.
   */
  public RefineableRelevantPredicatesComputer considerPredicatesAsRelevant(
      Block block,
      Set<AbstractionPredicate> predicates);

}
