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

import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * The class <code>PredicatePartition</code> represents a partition of predicates that are similar
 * to each other. Two predicates are similar if they have at least one variable in common.
 * <p/>
 * It is used by the {@link AbstractionManager} to generate a variable ordering for BDDs where a BDD
 * variable represents a predicate.
 */
public abstract class PredicatePartition {
  private static final UniqueIdGenerator partitionCounter = new UniqueIdGenerator();

  final LogManager logger;
  final int partitionID = partitionCounter.getFreshId();
  final FormulaManagerView fmgr;
  LinkedList<AbstractionPredicate> predicates;
  // mapping varID -> predicate in partition
  HashMap<Integer, AbstractionPredicate> varIDToPredicate;
  Solver solver;

  public PredicatePartition(FormulaManagerView fmgr, Solver solver, LogManager logger) {
    this.fmgr = fmgr;
    this.solver = solver;
    this.logger = logger;
    predicates = new LinkedList<>();
    varIDToPredicate = new HashMap<>();
  }

  /**
   * Inserts a new predicate in the partition using a certain insertion method.
   *
   * @param newPred the predicate that should be inserted.
   */
  abstract void insertPredicate(AbstractionPredicate newPred);

  /**
   * Merges this partition with another partition.
   *
   * @param newPreds the partition this partition is merged with.
   * @return the merge result.
   */
  abstract PredicatePartition merge(PredicatePartition newPreds);

  /**
   * Returns the partition id.
   *
   * @return the partition id.
   */
  public int getPartitionID() {
    return partitionID;
  }

  /**
   * Returns the predicates in this partition.
   *
   * @return the predicates in this partition.
   */
  public List<AbstractionPredicate> getPredicates() {
    return predicates;
  }

  /**
   * Returns the mapping of variable id to predicate.
   *
   * @return mapping of variable id to predicate.
   */
  public HashMap<Integer, AbstractionPredicate> getVarIDToPredicate() {
    return varIDToPredicate;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + partitionID;
    return result;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof PredicatePartition)) {
      return false;
    }
    PredicatePartition otherPartition = (PredicatePartition) object;
    return this.partitionID == otherPartition.getPartitionID();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    for (AbstractionPredicate predicate : getPredicates()) {
      sb.append(predicate.getVariableNumber());
      sb.append(',');
    }
    sb.append('}');
    return sb.toString();
  }
}