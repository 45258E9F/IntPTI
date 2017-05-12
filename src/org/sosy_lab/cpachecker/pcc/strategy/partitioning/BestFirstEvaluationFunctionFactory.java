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
package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import org.sosy_lab.cpachecker.core.interfaces.pcc.BestFirstEvaluationFunction;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedNode;

import java.util.Set;

public class BestFirstEvaluationFunctionFactory {

  private BestFirstEvaluationFunctionFactory() {
  }

  public enum BestFirstEvaluationFunctions {
    BREADTH_FIRST("breadth-first"),
    DEPTH_FIRST("depth-first"),
    BEST_IMPROVEMENT_FIRST("best-improvement-first");

    private final String description;

    BestFirstEvaluationFunctions(String pDescription) {
      description = pDescription;
    }

    @Override
    public String toString() {
      return description;
    }
  }

  public static BestFirstEvaluationFunction createEvaluationFunction(BestFirstEvaluationFunctions function) {
    switch (function) {
      case BREADTH_FIRST:
        return new BestFirstEvaluationFunction() {
          @Override
          public int computePriority(
              Set<Integer> partition, int priority, WeightedNode node,
              WeightedGraph wGraph) {
            return priority + 1; //expand next level nodes, when this level complete
          }
        };

      case DEPTH_FIRST:
        return new BestFirstEvaluationFunction() {
          @Override
          public int computePriority(
              Set<Integer> partition, int priority, WeightedNode node,
              WeightedGraph wGraph) {
            return priority
                - 1; //expand next level nodes, as next step (assumption: PriorityQueue preserves order of inserting)
          }
        };

      default:
        return new BestFirstEvaluationFunction() {
          @Override
          public int computePriority(
              Set<Integer> partition, int priority, WeightedNode node,
              WeightedGraph wGraph) {
            /*
             * if node not in partition it has cost of its weight for the actual partition ==> node-weight is gain
             * all of its successors which are not in the partition right now ==>  cost
             */
            Set<Integer> successors = wGraph.getIntSuccessors(node); //successors of this node
            successors.removeAll(partition); // successors, that are not in given partition
            int gain = node.getWeight();
            return WeightedGraph.computeWeight(successors, wGraph)
                - gain; //chance +/- since least priority is chosen first

          }
        };

    }
  }
}
