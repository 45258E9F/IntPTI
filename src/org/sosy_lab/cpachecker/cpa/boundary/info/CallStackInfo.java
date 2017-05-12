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
package org.sosy_lab.cpachecker.cpa.boundary.info;

import com.google.common.base.Objects;

import org.sosy_lab.cpachecker.cfa.model.CFANode;

import javax.annotation.Nonnull;

public final class CallStackInfo {

  private final CallStackInfo previousInfo;
  private final String currentFunction;
  private final CFANode callerNode;
  private final int depth;

  private CallStackInfo(
      CallStackInfo pPreviousInfo, @Nonnull String pCurrentFunction,
      @Nonnull CFANode pCallerNode) {
    previousInfo = pPreviousInfo;
    currentFunction = pCurrentFunction;
    callerNode = pCallerNode;
    if (previousInfo == null) {
      depth = 1;
    } else {
      depth = previousInfo.depth + 1;
    }
  }

  public static CallStackInfo of(
      CallStackInfo pPreviousInfo, String pCurrentFunction,
      CFANode pCallerNode) {
    return new CallStackInfo(pPreviousInfo, pCurrentFunction, pCallerNode);
  }


  public CallStackInfo getPreviousInfo() {
    return previousInfo;
  }

  public String getCurrentFunction() {
    return currentFunction;
  }

  public int getDepth() {
    return depth;
  }

  public CFANode getCallNode() {
    return callerNode;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(previousInfo, currentFunction, callerNode, depth);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !(obj instanceof CallStackInfo)) {
      return false;
    }
    CallStackInfo that = (CallStackInfo) obj;
    return Objects.equal(previousInfo, that.previousInfo) &&
        Objects.equal(currentFunction, that.currentFunction) &&
        Objects.equal(callerNode, that.callerNode) &&
        depth == that.depth;
  }
}
