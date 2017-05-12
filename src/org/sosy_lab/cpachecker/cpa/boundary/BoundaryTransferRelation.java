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
package org.sosy_lab.cpachecker.cpa.boundary;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop.CFASingleLoopTransformation;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop.ProgramCounterValueAssignmentEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.boundary.info.CallStackInfo;
import org.sosy_lab.cpachecker.cpa.boundary.info.LoopStackInfo;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.annotation.Nullable;

@Options(prefix = "cpa.boundary")
public class BoundaryTransferRelation extends SingleEdgeTransferRelation {

  @Option(secure = true, name = "callDepth", description = "Skip the function call if the depth "
      + "of call stack exceeds the given threshold")
  private int callBoundDepth = 0;

  @Option(secure = true, name = "loopIteration", description = "Skip the loop if the number of "
      + "iteration exceeds the given threshold")
  private int loopBoundIteration = -1;

  @Option(secure = true, name = "loopDepth", description = "Skip the loop if the depth of nesting"
      + " loop exceeds the given threshold")
  private int loopBoundDepth = 0;

  private static Map<CFAEdge, Loop> entry2Loop = ImmutableMap.of();
  private static Map<CFAEdge, Loop> exit2Loop = ImmutableMap.of();
  private static Multimap<CFANode, Loop> head2Loop = ImmutableMultimap.of();

  private final LogManagerWithoutDuplicates logger;

  public BoundaryTransferRelation(Configuration pConfig, LogManager pLogger, LoopStructure pLoops)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = new LogManagerWithoutDuplicates(pLogger);
    // construct basic data structures for loop information
    ImmutableMap.Builder<CFAEdge, Loop> loopEntry = ImmutableMap.builder();
    ImmutableMap.Builder<CFAEdge, Loop> loopExit = ImmutableMap.builder();
    ImmutableMultimap.Builder<CFANode, Loop> loopHead = ImmutableMultimap.builder();
    if (pLoops != null) {
      for (final Loop loop : pLoops.getAllLoops()) {
        Iterable<CFAEdge> incomingEdges = FluentIterable.from(loop.getIncomingEdges()).filter(
            new Predicate<CFAEdge>() {
              @Override
              public boolean apply(CFAEdge pCFAEdge) {
                if (pCFAEdge instanceof CFunctionReturnEdge) {
                  CFANode caller = ((CFunctionReturnEdge) pCFAEdge).getSummaryEdge()
                      .getPredecessor();
                  return !loop.getLoopNodes().contains(caller);
                }
                return true;
              }
            });
        Iterable<CFAEdge> outgoingEdges = filter(loop.getOutgoingEdges(), not(instanceOf
            (CFunctionCallEdge.class)));
        for (CFAEdge edge : incomingEdges) {
          loopEntry.put(edge, loop);
        }
        for (CFAEdge edge : outgoingEdges) {
          loopExit.put(edge, loop);
        }
        for (CFANode head : loop.getLoopHeads()) {
          loopHead.put(head, loop);
        }
      }
    }
    entry2Loop = loopEntry.build();
    exit2Loop = loopExit.build();
    head2Loop = loopHead.build();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, List<AbstractState> otherStates, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {

    BoundaryState currentState = (BoundaryState) state;
    CallStackInfo callStack = currentState.getCallStackInfo();
    LoopStackInfo loopStack = currentState.getLoopStackInfo();
    CFANode predecessor = cfaEdge.getPredecessor();
    CFANode successor = cfaEdge.getSuccessor();

    // STEP 0: prevent to walk along function summary statement edge, otherwise the ARG would
    // explode. In fact, we do not use this edge to analyze the summary.
    if (cfaEdge instanceof CFunctionSummaryStatementEdge) {
      return Collections.emptySet();
    }

    // STEP 1: handle function call edge
    if (cfaEdge instanceof CFunctionCallEdge) {
      // successor should be a function entry node, while predecessor is a caller node
      final String calledFunction = successor.getFunctionName();
      CallStackInfo newCallStack = CallStackInfo.of(callStack, calledFunction, predecessor);
      return Collections.singleton(new BoundaryState(newCallStack, loopStack, callBoundDepth,
          loopBoundIteration, loopBoundDepth, false));
    }

    // STEP 2: check if current edge is the exit of one loop
    Loop oldLoop = exit2Loop.get(cfaEdge);
    if (oldLoop != null) {
      assert oldLoop.equals(loopStack.getCurrentLoop());
      LoopStackInfo newLoopStack = loopStack.getPreviousInfo();
      // If current state is about to finish abstract execution, we should leave its successor
      // empty in order to prevent propagation of abstracted state.
      if (loopStack.underAbstractMode() && !newLoopStack.underAbstractMode()) {
        return Collections.emptySet();
      }
      loopStack = newLoopStack;
    }

    // STEP 3: handle a special case on assume edge by single loop transformation
    if (cfaEdge instanceof CAssumeEdge) {
      String predecessorFunction = predecessor.getFunctionName();
      String successorFunction = successor.getFunctionName();
      String currentFunction = callStack.getCurrentFunction();
      boolean stillInCallContext = successorFunction.equals(currentFunction);
      boolean isArtificialPCVEdge = cfaEdge instanceof ProgramCounterValueAssignmentEdge;
      boolean isSuccessorArtificialPCNode = successorFunction.equals(CFASingleLoopTransformation
          .ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME);
      boolean isPredecessorArtificialPCNode = predecessorFunction.equals
          (CFASingleLoopTransformation.ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME);
      boolean isFunctionTransition = !successorFunction.equals(predecessorFunction);
      if (!stillInCallContext &&
          currentFunction.equals(CFASingleLoopTransformation
              .ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME) &&
          ((!isSuccessorArtificialPCNode && isArtificialPCVEdge) ||
              isPredecessorArtificialPCNode && isFunctionTransition)) {
        return Collections.emptySet();
      }
    }

    // STEP 4: check if current edge leads to the entry of one loop
    boolean isDefaultLoopFlag = false;
    Loop newLoop = entry2Loop.get(cfaEdge);
    if (newLoop != null) {
      loopStack = LoopStackInfo.of(loopStack, newLoop, 0);
      if (loopStack.underAbstractMode()) {
        isDefaultLoopFlag = true;
      }
    }
    // STEP 5: update iteration number
    Collection<Loop> loops = head2Loop.get(successor);
    if (loops.size() > 1) {
      logger.log(Level.SEVERE, "The CFA node " + successor + " is the head of " + loops.size() +
          " loops");
      throw new IllegalStateException("A CFA node should be the head of at most one loop");
    }
    if (loops.contains(loopStack.getCurrentLoop())) {
      // we are going to enter the loop, then iteration number increments by 1
      int newIteration = loopStack.getLoopIteration();
      if (loopStack.underAbstractMode() && newIteration > 0) {
        // it is not necessary to analyze a loop for multiple passes when under abstract
        // execution mode
        return Collections.emptySet();
      }
      newIteration++;
      loopStack = LoopStackInfo.of(loopStack.getPreviousInfo(), loopStack.getCurrentLoop(),
          newIteration);
    }

    // STEP 6: handle function return edge
    if (cfaEdge instanceof CFunctionReturnEdge) {
      if (isWildcard(callStack)) {
        return Collections.singleton(currentState);
      } else {
        CFANode callerNode = successor.getEnteringSummaryEdge().getPredecessor();
        if (callerNode == null || !callerNode.equals(callStack.getCallNode())) {
          // caller node and the function return edge do not match
          return Collections.emptySet();
        }
        CallStackInfo oldCallStack = callStack.getPreviousInfo();
        return Collections.singleton(new BoundaryState(oldCallStack, loopStack, callBoundDepth,
            loopBoundIteration, loopBoundDepth, isDefaultLoopFlag));
      }
    }

    return Collections.singleton(new BoundaryState(callStack, loopStack, callBoundDepth,
        loopBoundIteration, loopBoundDepth, isDefaultLoopFlag));
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      List<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision) throws CPATransferException, InterruptedException {
    return null;
  }

  /* ***************** */
  /* auxiliary methods */
  /* ***************** */

  /**
   * Check if the given call stack should be treated as a wildcard stack.
   */
  private boolean isWildcard(final CallStackInfo pCallStack) {
    final String functionName = pCallStack.getCurrentFunction();
    if (functionName.equals(CFASingleLoopTransformation.ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME)) {
      return true;
    }
    CFANode callerNode = pCallStack.getCallNode();
    // e.g. main function
    if (callerNode instanceof FunctionEntryNode && callerNode.getFunctionName().equals
        (functionName)) {
      return false;
    }
    // e.g. normal function call
    if (CFAUtils.successorsOf(callerNode).filter(FunctionEntryNode.class).anyMatch(
        new Predicate<FunctionEntryNode>() {
          @Override
          public boolean apply(FunctionEntryNode pFunctionEntryNode) {
            return pFunctionEntryNode.getFunctionName().equals(functionName);
          }
        })) {
      return false;
    }
    return true;
  }

  /* ************ */
  /* query method */
  /* ************ */

  @Nullable
  static LoopStackInfo updateLoopInfoFollowingStatement(CFAEdge pEdge, LoopStackInfo pInfo) {
    LoopStackInfo loopInfo = pInfo;
    Loop newLoop = entry2Loop.get(pEdge);
    if (newLoop != null) {
      loopInfo = LoopStackInfo.of(loopInfo, newLoop, 0);
    }
    Collection<Loop> loops = head2Loop.get(pEdge.getSuccessor());
    if (loops.size() > 1) {
      throw new IllegalStateException("A CFA node should be the head of at most one loop");
    }
    if (loops.contains(loopInfo.getCurrentLoop())) {
      int newIteration = loopInfo.getLoopIteration() + 1;
      if (loopInfo.underAbstractMode() && newIteration > 1) {
        // That means we should not take this edge.
        return null;
      }
      loopInfo = LoopStackInfo.of(loopInfo.getPreviousInfo(), loopInfo.getCurrentLoop(),
          newIteration);
    }
    return loopInfo;
  }

  /* ************* */
  /* query methods */
  /* ************* */

}