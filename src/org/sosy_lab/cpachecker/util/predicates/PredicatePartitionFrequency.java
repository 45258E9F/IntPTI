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
package org.sosy_lab.cpachecker.util.predicates;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * The class <code>PredicatePartitionFrequency</code> represents a concrete partition of predicates
 * and hence it extends {@link PredicatePartition}.
 * <p/>
 * It is used by the {@link AbstractionManager} to generate a variable ordering for BDDs where a BDD
 * variable represents a predicate.
 */
public class PredicatePartitionFrequency extends PredicatePartition {
  private PriorityQueue<AbstractionPredicate> predicatesSortedByVarFrequency;

  public PredicatePartitionFrequency(FormulaManagerView fmgr, Solver solver, LogManager logger) {
    super(fmgr, solver, logger);
    predicatesSortedByVarFrequency = new PriorityQueue<>(1, new Comparator<AbstractionPredicate>() {
      @Override
      public int compare(AbstractionPredicate pred1, AbstractionPredicate pred2) {
        return pred2.getVariableNumber() - pred1.getVariableNumber();
      }
    });
  }

  /**
   * Inserts a new predicate such that predicates with a higher number of variables are left
   * and predicates with a lower number of variables are right of the new predicate.
   *
   * @param newPred the new predicate to be inserted in the partition
   */
  @Override
  public void insertPredicate(AbstractionPredicate newPred) {
    this.varIDToPredicate.put(newPred.getVariableNumber(), newPred);
    predicatesSortedByVarFrequency.add(newPred);
  }

  @Override
  public PredicatePartition merge(PredicatePartition newPreds) {
    if (this.partitionID != newPreds.getPartitionID()) {
      // merge the mappings varIDToPredicate of the two partitions.
      // this has to be done no matter which insertion strategy is used.
      this.varIDToPredicate.putAll(newPreds.getVarIDToPredicate());

      // insert all predicates of the other partition at the right place in the priority queue of this partition
      this.predicatesSortedByVarFrequency.addAll(newPreds.getPredicates());
    }

    return this;
  }

  @Override
  public List<AbstractionPredicate> getPredicates() {
    if (predicatesSortedByVarFrequency.size() != 0) {
      while (!predicatesSortedByVarFrequency.isEmpty()) {
        predicates.add(predicatesSortedByVarFrequency.poll());
      }
    }
    return predicates;
  }
}