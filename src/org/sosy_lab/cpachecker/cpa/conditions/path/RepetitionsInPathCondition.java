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
package org.sosy_lab.cpachecker.cpa.conditions.path;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.NoOpReducer;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.assumptions.PreventingHeuristic;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import java.io.PrintStream;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A {@link PathCondition} where the condition is based on the number of appearances
 * of edges in a path. I.e., if the threshold is 3, a path is cut off as soon
 * as any edge is seen the third time in the path.
 * However, only FunctionCallEdges and outgoing edges of a loop head are tracked.
 */
@Options(prefix = "cpa.conditions.path.repetitions")
public class RepetitionsInPathCondition implements PathCondition, Statistics {

  @Option(secure = true, description = "maximum repetitions of any edge in a path (-1 for infinite)",
      name = "limit")
  @IntegerOption(min = -1)
  private int threshold = -1;

  private int increaseThresholdBy = 0;

  private int maxRepetitionsInPath = 0;


  public RepetitionsInPathCondition(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  @Override
  public AvoidanceReportingState getInitialState(CFANode pNode) {
    return new RepetitionsInPathConditionState(ImmutableMap.<CFAEdge, Integer>of(), threshold,
        false);
  }

  private boolean isInteresting(CFAEdge edge) {
    return (edge.getEdgeType() == CFAEdgeType.FunctionCallEdge)
        || (edge.getPredecessor().isLoopStart());
  }

  @Override
  public AvoidanceReportingState getAbstractSuccessor(AbstractState pState, CFAEdge pEdge) {
    RepetitionsInPathConditionState current = (RepetitionsInPathConditionState) pState;

    if (!isInteresting(pEdge)) {
      return current;
    }

    if (current.thresholdReached) {
      return current;
    }

    Integer repetitions = firstNonNull(current.frequencyMap.get(pEdge), 0);
    repetitions++;

    boolean thresholdReached = (threshold >= 0) && (repetitions >= threshold);

    maxRepetitionsInPath = Math.max(repetitions, maxRepetitionsInPath);

    Map<CFAEdge, Integer> newFrequencyMap = Maps.newHashMap(current.frequencyMap);
    newFrequencyMap.put(pEdge, repetitions);

    return new RepetitionsInPathConditionState(newFrequencyMap, threshold, thresholdReached);
  }

  @Override
  public boolean adjustPrecision() {
    if (threshold == -1) {
      // set the initial threshold value
      // TODO PW: Do this calculations make sense? I just copied them from RepetitionsInPathHeuristicsPrecision.
      threshold = maxRepetitionsInPath / 5;
      increaseThresholdBy = threshold;

    } else {
      threshold = threshold + increaseThresholdBy;
    }
    return true;
  }

  @Override
  public String getName() {
    return "Repetitions in path condition";
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
    out.println("Maximum repetitions in a path: " + maxRepetitionsInPath);
    out.println("Threshold value:               " + threshold);
  }


  private static class RepetitionsInPathConditionState
      implements AbstractState, AvoidanceReportingState {

    private final ImmutableMap<CFAEdge, Integer> frequencyMap;
    private final int threshold;
    private final boolean thresholdReached;

    private RepetitionsInPathConditionState(
        Map<CFAEdge, Integer> pFrequencyMap,
        int pThreshold, boolean pThresholdReached) {
      frequencyMap = ImmutableMap.copyOf(pFrequencyMap);
      threshold = pThreshold;
      thresholdReached = pThresholdReached;
    }

    @Override
    public boolean mustDumpAssumptionForAvoidance() {
      return thresholdReached;
    }

    @Override
    public BooleanFormula getReasonFormula(FormulaManagerView pMgr) {
      return PreventingHeuristic.REPETITIONSINPATH.getFormula(pMgr, threshold);
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      for (Entry<CFAEdge, Integer> entry : frequencyMap.entrySet()) {
        builder.append(entry.getValue())
            .append("x(")
            .append(entry.getKey())
            .append(") ");
      }
      return builder.toString();
    }

    @Override
    public boolean isEqualTo(AbstractState other) {
      if (other == this) {
        return true;
      }
      if (!(other instanceof RepetitionsInPathConditionState)) {
        return false;
      }
      RepetitionsInPathConditionState that = (RepetitionsInPathConditionState) other;
      return Objects.equal(frequencyMap, that.frequencyMap) &&
          Objects.equal(threshold, that.threshold) &&
          Objects.equal(thresholdReached, that.thresholdReached);
    }
  }

  @Override
  public Reducer getReducer() {
    return NoOpReducer.getInstance();
  }

}
