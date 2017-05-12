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
package org.sosy_lab.cpachecker.cpa.callstack;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

public class CallstackState implements AbstractState, Partitionable, AbstractQueryableState,
                                       Serializable {

  private static final long serialVersionUID = 3629687385150064994L;

  protected final CallstackState previousState;
  protected final String currentFunction;
  protected transient CFANode callerNode;
  private final int depth;

  public CallstackState(
      CallstackState pPreviousElement, @Nonnull String pFunction,
      @Nonnull CFANode pCallerNode) {

    previousState = pPreviousElement;
    currentFunction = checkNotNull(pFunction);
    callerNode = checkNotNull(pCallerNode);

    if (pPreviousElement == null) {
      depth = 1;
    } else {
      depth = pPreviousElement.getDepth() + 1;
    }
  }

  public CallstackState getPreviousState() {
    return previousState;
  }

  public String getCurrentFunction() {
    return currentFunction;
  }

  public CFANode getCallNode() {
    return callerNode;
  }

  public int getDepth() {
    return depth;
  }

  /**
   * for logging and debugging
   */
  private List<String> getStack() {
    final List<String> stack = new ArrayList<>();
    CallstackState state = this;
    while (state != null) {
      stack.add(state.getCurrentFunction());
      state = state.getPreviousState();
    }
    return Lists.reverse(stack);
  }

  @Override
  public Object getPartitionKey() {
    return this;
  }

  @Override
  public String toString() {
    return "Function " + getCurrentFunction()
        + " called from node " + getCallNode()
        + ", stack depth " + getDepth()
        + " [" + Integer.toHexString(super.hashCode())
        + "], stack " + getStack();
  }

  public boolean sameStateInProofChecking(CallstackState pOther) {
    if (pOther.callerNode == callerNode
        && pOther.depth == depth
        && pOther.currentFunction.equals(currentFunction)
        && (pOther.previousState == previousState || (previousState != null
        && pOther.previousState != null && previousState
        .sameStateInProofChecking(pOther.previousState)))) {
      return true;
    }
    return false;
  }

  @Override
  public String getCPAName() {
    return "Callstack";
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    return false;
  }

  @Override
  public boolean isEqualTo(AbstractState other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof CallstackState)) {
      return false;
    }
    CallstackState that = (CallstackState) other;
    return Objects.equal(currentFunction, that.currentFunction) &&
        Objects.equal(callerNode, that.callerNode) &&
        Objects.equal(depth, that.depth) &&
        ((previousState == null) == (that.previousState == null) &&
            (previousState == null || previousState.isEqualTo(that.previousState)));
  }

  @Override
  public Object evaluateProperty(String pProperty) throws InvalidQueryException {
    if (pProperty.compareToIgnoreCase("caller") == 0) {
      if (callerNode != null) {
        return this.callerNode.getFunctionName();
      } else {
        return "";
      }
    }

    throw new InvalidQueryException(
        String.format("Evaluating %s not supported by %s", pProperty, this.getClass()
            .getCanonicalName()));
  }

  @Override
  public void modifyProperty(String pModification) throws InvalidQueryException {
    throw new InvalidQueryException(
        "modifyProperty not implemented by " + this.getClass().getCanonicalName());
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    out.writeInt(callerNode.getNodeNumber());
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    int nodeNumber = in.readInt();
    callerNode = GlobalInfo.getInstance().getCFAInfo().get().getNodeByNodeNumber(nodeNumber);
  }
}
