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

/**
 * A {@link PathCondition} where the condition is based on the number of assume
 * edges seen so far on the current path.
 */
@Options(prefix = "cpa.conditions.path.assumeedges")
public class AssumeEdgesInPathCondition implements PathCondition, Statistics {

  @Option(secure = true, description = "maximum number of assume edges length (-1 for infinite)",
      name = "limit")
  @IntegerOption(min = -1)
  private int threshold = -1;

  private int increaseThresholdBy = 0;

  private int maxAssumeEdgesInPath = 0;


  public AssumeEdgesInPathCondition(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  @Override
  public AvoidanceReportingState getInitialState(CFANode pNode) {
    return new RepetitionsInPathConditionState(0, false);
  }

  @Override
  public AvoidanceReportingState getAbstractSuccessor(AbstractState pState, CFAEdge pEdge) {
    RepetitionsInPathConditionState current = (RepetitionsInPathConditionState) pState;

    if (pEdge.getEdgeType() != CFAEdgeType.AssumeEdge) {
      return current;
    }

    if (current.thresholdReached) {
      return current;
    }

    int assumeEdgesInPath = current.assumeEdgesInPath + 1;
    boolean thresholdReached = (threshold >= 0) && (assumeEdgesInPath >= threshold);

    maxAssumeEdgesInPath = Math.max(assumeEdgesInPath, maxAssumeEdgesInPath);

    return new RepetitionsInPathConditionState(assumeEdgesInPath, thresholdReached);
  }

  @Override
  public boolean adjustPrecision() {
    if (threshold == -1) {
      // set the initial threshold value
      // TODO PW: Do this calculations make sense? I just copied them from AssumeEdgesInPathHeuristicsPrecision.
      threshold = maxAssumeEdgesInPath / 5;
      increaseThresholdBy = threshold / 4;

    } else {
      threshold = threshold + increaseThresholdBy;
    }
    return true;
  }

  @Override
  public String getName() {
    return "Assume edges in path condition";
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
    out.println("Maximum length of a path: " + maxAssumeEdgesInPath);
    out.println("Threshold value:          " + threshold);
  }


  private static class RepetitionsInPathConditionState
      implements AbstractState, AvoidanceReportingState {

    private final int assumeEdgesInPath;
    private final boolean thresholdReached;

    private RepetitionsInPathConditionState(int pPathLength, boolean pThresholdReached) {
      assumeEdgesInPath = pPathLength;
      thresholdReached = pThresholdReached;
    }

    @Override
    public boolean mustDumpAssumptionForAvoidance() {
      return thresholdReached;
    }

    @Override
    public BooleanFormula getReasonFormula(FormulaManagerView pMgr) {
      return PreventingHeuristic.ASSUMEEDGESINPATH.getFormula(pMgr, assumeEdgesInPath);
    }

    @Override
    public String toString() {
      return "path length: " + assumeEdgesInPath
          + (thresholdReached ? " (threshold reached)" : "");
    }

    @Override
    public boolean isEqualTo(AbstractState other) {
      if (other == this) {
        return true;
      }
      if (other instanceof RepetitionsInPathConditionState) {
        RepetitionsInPathConditionState that = (RepetitionsInPathConditionState) other;
        return assumeEdgesInPath == that.assumeEdgesInPath &&
            thresholdReached == that.thresholdReached;
      } else {
        return false;
      }
    }
  }


  @Override
  public Reducer getReducer() {
    return NoOpReducer.getInstance();
  }
}
