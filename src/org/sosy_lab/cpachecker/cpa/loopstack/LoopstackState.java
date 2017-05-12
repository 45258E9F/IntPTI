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
package org.sosy_lab.cpachecker.cpa.loopstack;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.SwitchableGraphable;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.assumptions.PreventingHeuristic;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class LoopstackState implements AbstractState, Partitionable, AvoidanceReportingState,
                                       SwitchableGraphable {

  /**
   * Parent loop.
   */
  private final
  @Nullable
  LoopstackState previousState;

  /**
   * The loop we are currently in.
   */
  private final
  @Nullable
  Loop loop;

  /**
   * The depth of the stack of LoopstackStates.
   */
  private final int depth;

  /**
   * Number of iterations within the current loop.
   * Starts at one for any state inside the loop returned by
   * LoopstackTransferRelation.
   */
  private final int iteration;
  private final boolean stop;
  private final boolean loopCounterAbstracted;

  private int hashCache = 0;

  public LoopstackState(
      LoopstackState previousElement, Loop loop, int iteration, boolean stop,
      boolean pLoopCounterAbstracted) {
    this.previousState = checkNotNull(previousElement);
    this.loop = checkNotNull(loop);
    this.depth = previousElement.getDepth() + 1;
    checkArgument(iteration >= 0);
    this.iteration = iteration;
    this.stop = stop;
    loopCounterAbstracted = pLoopCounterAbstracted;
  }

  public LoopstackState() {
    previousState = null;
    loop = null;
    depth = 0;
    iteration = 0;
    stop = false;
    loopCounterAbstracted = false;
  }

  public boolean isLoopCounterAbstracted() {
    return loopCounterAbstracted;
  }

  public LoopstackState getPreviousState() {
    return previousState;
  }

  public Loop getLoop() {
    return loop;
  }

  public int getDepth() {
    return depth;
  }

  public int getIteration() {
    return iteration;
  }

  @Override
  public Object getPartitionKey() {
    return this;
  }

  @Override
  public boolean mustDumpAssumptionForAvoidance() {
    return stop;
  }

  @Override
  public String toString() {
    if (loop == null) {
      return "Loop stack empty";
    } else {
      return " Loop starting at node " + loop.getLoopHeads() + " in iteration " + iteration
          + ", stack depth " + depth
          + " [" + Integer.toHexString(super.hashCode()) + "]";
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof LoopstackState)) {
      return false;
    }

    LoopstackState other = (LoopstackState) obj;
    return (this.previousState == other.previousState)
        && (this.iteration == other.iteration)
        && (this.loop == other.loop);
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache = Objects.hashCode(iteration, loop, previousState);
    }
    return hashCache;
  }

  @Override
  public BooleanFormula getReasonFormula(FormulaManagerView manager) {
    BooleanFormulaManager bfmgr = manager.getBooleanFormulaManager();
    if (stop) {
      return PreventingHeuristic.LOOPITERATIONS.getFormula(manager, iteration);
    } else {
      return bfmgr.makeBoolean(true);
    }
  }

  /**
   * Print the identifier of current loop stack state.
   * Each loop stack can be formalized as N_M were N is the iteration number and M is the stack
   * depth.
   * The identifier has the form of N_M:N_M:N_M... where the oldest loop comes first.
   * If the loop stack is empty, then we print nothing.
   */
  public String getLoopStackIdentifier() {
    if (loop == null) {
      return "";
    } else {
      List<String> idList = new ArrayList<>();
      getLoopStackIdentifier(this, idList);
      return Joiner.on(':').join(idList);
    }
  }

  private static void getLoopStackIdentifier(LoopstackState pState, List<String> idList) {
    if (pState != null) {
      getLoopStackIdentifier(pState.previousState, idList);
      idList.add(String.format("%d_%d", pState.iteration, pState.depth));
    }
  }

  @Override
  public boolean getActiveStatus() {
    return true;
  }

  @Override
  public String toDOTLabel() {
    return "Iteration: " + iteration + "\nDepth: " + depth;
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }
}
