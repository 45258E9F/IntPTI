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
package org.sosy_lab.cpachecker.cpa.conditions.path;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
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
 * A {@link PathCondition} where the condition is the length of the current path.
 */
@Options(prefix = "cpa.conditions.path.length")
public class PathLengthCondition implements PathCondition, Statistics {

  @Option(secure = true, description = "maximum path length (-1 for infinite)",
      name = "limit")
  @IntegerOption(min = -1)
  private int threshold = -1;

  private int increaseThresholdBy = 0;

  private int maxPathLength = 0;


  public PathLengthCondition(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  @Override
  public AvoidanceReportingState getInitialState(CFANode pNode) {
    return new PathLengthConditionState(0, false);
  }

  @Override
  public AvoidanceReportingState getAbstractSuccessor(AbstractState pState, CFAEdge pEdge) {

    PathLengthConditionState current = (PathLengthConditionState) pState;
    if (current.thresholdReached) {
      return current;
    }

    int pathLength = current.pathLength + 1;
    boolean thresholdReached = (threshold >= 0) && (pathLength >= threshold);

    maxPathLength = Math.max(pathLength, maxPathLength);

    return new PathLengthConditionState(pathLength, thresholdReached);
  }

  @Override
  public boolean adjustPrecision() {
    if (threshold == -1) {
      // set the initial threshold value
      // TODO PW: Do this calculations make sense? I just copied them from PathLengthHeuristicsPrecision.
      threshold = maxPathLength / 5;
      increaseThresholdBy = threshold / 4;

    } else {
      threshold = threshold + increaseThresholdBy;
    }
    return true;
  }

  @Override
  public String getName() {
    return "Path length condition";
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
    out.println("Maximum length of a path: " + maxPathLength);
    out.println("Threshold value:          " + threshold);
  }


  private static class PathLengthConditionState implements AbstractState, AvoidanceReportingState {

    private final int pathLength;
    private final boolean thresholdReached;

    private PathLengthConditionState(int pPathLength, boolean pThresholdReached) {
      pathLength = pPathLength;
      thresholdReached = pThresholdReached;
    }

    @Override
    public boolean mustDumpAssumptionForAvoidance() {
      return thresholdReached;
    }

    @Override
    public BooleanFormula getReasonFormula(FormulaManagerView pMgr) {
      return PreventingHeuristic.PATHLENGTH.getFormula(pMgr, pathLength);
    }

    @Override
    public String toString() {
      return "path length: " + pathLength
          + (thresholdReached ? " (threshold reached)" : "");
    }

    @Override
    public boolean isEqualTo(AbstractState other) {
      if (this == other) {
        return true;
      }
      if (other instanceof PathLengthConditionState) {
        PathLengthConditionState that = (PathLengthConditionState) other;
        return pathLength == that.pathLength && thresholdReached == that.thresholdReached;
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
