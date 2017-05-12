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
package org.sosy_lab.cpachecker.cpa.boundary;

import static org.sosy_lab.cpachecker.cpa.boundary.BoundaryState.BoundaryFlag.FUNCTION;
import static org.sosy_lab.cpachecker.cpa.boundary.BoundaryState.BoundaryFlag.LOOP;
import static org.sosy_lab.cpachecker.cpa.boundary.BoundaryState.BoundaryFlag.NONE;
import static org.sosy_lab.cpachecker.cpa.boundary.info.LoopBoundedInfo.LoopOutOfBoundReason.MAX_DEPTH_REACHED;
import static org.sosy_lab.cpachecker.cpa.boundary.info.LoopBoundedInfo.LoopOutOfBoundReason.MAX_ITERATION_REACHED;
import static org.sosy_lab.cpachecker.cpa.boundary.info.LoopBoundedInfo.LoopOutOfBoundReason.N_A;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.SwitchableGraphable;
import org.sosy_lab.cpachecker.core.interfaces.summary.SummaryAcceptableState;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryInstance;
import org.sosy_lab.cpachecker.cpa.boundary.info.BoundedInfo;
import org.sosy_lab.cpachecker.cpa.boundary.info.CallStackInfo;
import org.sosy_lab.cpachecker.cpa.boundary.info.EmptyBoundedInfo;
import org.sosy_lab.cpachecker.cpa.boundary.info.FunctionBoundedInfo;
import org.sosy_lab.cpachecker.cpa.boundary.info.LoopBoundedInfo;
import org.sosy_lab.cpachecker.cpa.boundary.info.LoopBoundedInfo.LoopOutOfBoundReason;
import org.sosy_lab.cpachecker.cpa.boundary.info.LoopStackInfo;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BoundaryState implements AbstractState, Partitionable, SwitchableGraphable,
                                      SummaryAcceptableState {

  public enum BoundaryFlag {
    NONE,
    LOOP,
    FUNCTION
  }

  private final CallStackInfo callStackInfo;
  private final LoopStackInfo loopStackInfo;

  private BoundaryFlag flag;
  private LoopOutOfBoundReason loopReason = N_A;

  BoundaryState(
      @Nonnull CallStackInfo pCallStackInfo,
      @Nonnull LoopStackInfo pLoopStackInfo) {
    callStackInfo = pCallStackInfo;
    loopStackInfo = pLoopStackInfo;
    flag = NONE;
  }

  BoundaryState(
      @Nonnull CallStackInfo pCallStackInfo,
      @Nonnull LoopStackInfo pLoopStackInfo,
      final int callBoundDepth,
      final int loopBoundIteration,
      final int loopBoundDepth,
      boolean isDefaultLoopFlag) {
    callStackInfo = pCallStackInfo;
    loopStackInfo = pLoopStackInfo;
    check(callBoundDepth, loopBoundIteration, loopBoundDepth, isDefaultLoopFlag);
  }

  @Override
  public Object getPartitionKey() {
    return this;
  }

  /* ******* */
  /* getters */
  /* ******* */

  CallStackInfo getCallStackInfo() {
    return callStackInfo;
  }

  LoopStackInfo getLoopStackInfo() {
    return loopStackInfo;
  }

  public BoundaryFlag getFlag() {
    return flag;
  }

  public BoundedInfo<?> getBoundedInfo(CFANode predecessor, CFANode successor) {
    switch (flag) {
      case NONE:
        return EmptyBoundedInfo.EMPTY;
      case FUNCTION:
        return FunctionBoundedInfo.of(callStackInfo.getCurrentFunction(), predecessor);
      case LOOP:
        return LoopBoundedInfo.of(loopStackInfo.getCurrentLoop(), successor, loopReason);
      default:
        throw new UnsupportedOperationException("Unrecognized boundary flag: " + flag);
    }
  }

  public int getCallDepth() {
    return callStackInfo.getDepth();
  }

  public int getLoopDepth() {
    return loopStackInfo.getLoopDepth();
  }

  @Nullable
  public Loop getCurrentLoop() {
    return loopStackInfo.getCurrentLoop();
  }

  public int getLoopIteration() {
    return loopStackInfo.getLoopIteration();
  }

  public boolean underAbstractExecution() {
    return loopStackInfo.underAbstractMode();
  }

  /* ************* */
  /* boundary flag */
  /* ************* */

  /**
   * Check if current boundary state exceeds the specified threshold.
   *
   * @param callBoundDepth     maximum level of call stacks
   * @param loopBoundIteration maximum number of loop iterations
   * @param loopBoundDepth     maximum level of nesting loop
   * @param isDefaultLoopFlag  whether the loop flag is set by default, which is used when we enter
   *                           an new loop and the current boundary state is under abstract
   *                           execution mode
   */
  private void check(
      int callBoundDepth, int loopBoundIteration, int loopBoundDepth, boolean
      isDefaultLoopFlag) {
    if (callBoundDepth > 0) {
      if (callStackInfo.getDepth() > callBoundDepth) {
        flag = FUNCTION;
        return;
      }
    }
    // FIX: no concrete loop analysis is allowed
    if (loopBoundIteration >= 0) {
      if (!loopStackInfo.underAbstractMode() && loopStackInfo.getLoopIteration() >
          loopBoundIteration) {
        flag = LOOP;
        loopReason = MAX_ITERATION_REACHED;
        return;
      }
    }
    if (loopBoundDepth > 0) {
      if (loopStackInfo.getLoopDepth() > loopBoundDepth) {
        flag = LOOP;
        loopReason = MAX_DEPTH_REACHED;
        return;
      }
    }
    if (isDefaultLoopFlag) {
      flag = LOOP;
      loopReason = MAX_ITERATION_REACHED;
    } else {
      flag = NONE;
    }
  }

  /* ********* */
  /* overrides */
  /* ********* */

  @Override
  public int hashCode() {
    return Objects.hashCode(callStackInfo, loopStackInfo, flag);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !(obj instanceof BoundaryState)) {
      return false;
    }
    BoundaryState that = (BoundaryState) obj;
    return Objects.equal(callStackInfo, that.callStackInfo) &&
        Objects.equal(loopStackInfo, that.loopStackInfo) &&
        Objects.equal(flag, that.flag);
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    return equals(other);
  }

  /* ********** */
  /* identifier */
  /* ********** */

  private static final String template = "L[%s]_C[%s]";

  /**
   * Serialize the boundary state in a very simplified string form. For now we only serialize the
   * loop stack state. Each loop stack is represented as N_M where N is the iteration number.
   * Though it is not sufficient to be the UID of a boundary state, it is sufficient to
   * distinguish some states for memory allocation analysis.
   */
  public String getBoundaryIdentifier() {
    List<String> loopIdList = new ArrayList<>();
    loopStackInfo.getIdentifier(loopIdList);
    return String.format(template, Joiner.on(':').join(loopIdList), "");
  }

  /* ******************* */
  /* summary application */
  /* ******************* */

  @Override
  public Collection<? extends AbstractState> applyFunctionSummary(
      List<SummaryInstance> pSummaryList,
      CFAEdge inEdge,
      CFAEdge outEdge,
      List<AbstractState> pOtherStates) throws CPATransferException {
    // UPDATE: be aware! It is possible that a function summary statement edge is the entry
    // edge of certain loop. Consider the following program as an example:
    //
    // for(x = foo(); stop(x); x = increment(x)) {...}
    //
    // Here the implementation of foo() is known, and we can either enter foo() or skip foo()
    // by applying function summary. However, if we apply the function summary for foo(), we
    // enter the loop after skipping the function, and the new state should have updated loop
    // stack. Therefore, before updating call stack and returning the resultant boundary state,
    // we should perform an additional check to examine whether it is necessary to update loop
    // stack information.
    BoundaryState state = this;
    CFunctionCallEdge entryEdge = (CFunctionCallEdge) inEdge;
    CFunctionSummaryEdge summaryEdge = entryEdge.getSummaryEdge();
    CFAEdge statementEdge = CFAUtils.getConnectingEdge(summaryEdge.getPredecessor(), summaryEdge
        .getSuccessor(), CStatementEdge.class);
    if (statementEdge != null) {
      LoopStackInfo newLoopInfo = BoundaryTransferRelation.updateLoopInfoFollowingStatement
          (statementEdge, loopStackInfo);
      if (newLoopInfo == null) {
        return Collections.emptySet();
      }
      if (!newLoopInfo.equals(loopStackInfo)) {
        state = new BoundaryState(callStackInfo, newLoopInfo);
      }
    }
    return Collections.singleton(state);
  }

  @Override
  public Multimap<CFAEdge, AbstractState> applyExternalLoopSummary(
      List<SummaryInstance> pSummaryList,
      CFAEdge inEdge,
      Collection<CFAEdge> outEdges,
      List<AbstractState> pOtherStates) throws CPATransferException {
    LoopStackInfo newLoopStack = loopStackInfo.getPreviousInfo();
    BoundaryState newState = new BoundaryState(callStackInfo, newLoopStack);
    Multimap<CFAEdge, AbstractState> newStates = HashMultimap.create();
    for (CFAEdge outEdge : outEdges) {
      newStates.put(outEdge, newState);
    }
    return newStates;
  }

  @Override
  public Collection<? extends AbstractState> applyInternalLoopSummary(
      List<SummaryInstance> pSummaryList, CFAEdge inEdge, List<AbstractState> pOtherStates)
      throws CPATransferException {
    // we should mark the new boundary state as under abstract execution, otherwise the analysis
    // could not terminate
    if (loopStackInfo.underAbstractMode()) {
      return Collections.singleton(this);
    }
    BoundaryState newState = new BoundaryState(callStackInfo, LoopStackInfo.of(loopStackInfo,
        true));
    return Collections.singleton(newState);
  }

  @Override
  public String toDOTLabel() {
    if (loopStackInfo.getCurrentLoop() != null) {
      return String.valueOf(callStackInfo.getDepth() + " " + loopStackInfo.getLoopDepth() + " " +
          loopStackInfo.getLoopIteration());
    }
    return String.valueOf(callStackInfo.getDepth());
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public boolean getActiveStatus() {
    return true;
  }

}
