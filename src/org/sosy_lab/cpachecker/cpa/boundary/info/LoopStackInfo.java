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
package org.sosy_lab.cpachecker.cpa.boundary.info;

import com.google.common.base.Objects;

import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import java.util.List;

import javax.annotation.Nonnull;

public final class LoopStackInfo {

  private final LoopStackInfo previousInfo;
  private final Loop currentLoop;
  private final int loopDepth;
  private final int loopIteration;

  private final boolean abstractMode;

  private LoopStackInfo() {
    previousInfo = null;
    currentLoop = null;
    loopDepth = 0;
    loopIteration = 0;
    abstractMode = false;
  }

  private LoopStackInfo(LoopStackInfo pInfo) {
    previousInfo = pInfo.previousInfo;
    currentLoop = pInfo.currentLoop;
    loopDepth = pInfo.loopDepth;
    loopIteration = pInfo.loopIteration;
    abstractMode = pInfo.abstractMode;
  }

  private LoopStackInfo(LoopStackInfo pPreviousInfo, Loop pLoop, int pLoopIteration) {
    previousInfo = pPreviousInfo;
    currentLoop = pLoop;
    loopDepth = (previousInfo == null) ? 1 : previousInfo.loopDepth + 1;
    loopIteration = pLoopIteration;
    abstractMode = previousInfo != null && previousInfo.abstractMode;
  }

  /**
   * The only method to manually specify the abstract mode. Other constructors could only
   * transfer the abstract execution mode to the new state.
   */
  private LoopStackInfo(LoopStackInfo pInfo, boolean pAbstractMode) {
    previousInfo = pInfo.previousInfo;
    currentLoop = pInfo.currentLoop;
    loopDepth = pInfo.loopDepth;
    loopIteration = pInfo.loopIteration;
    abstractMode = pAbstractMode;
  }

  public static LoopStackInfo of() {
    return new LoopStackInfo();
  }

  public static LoopStackInfo of(LoopStackInfo pPreviousInfo, Loop pLoop, int pLoopIteration) {
    return new LoopStackInfo(pPreviousInfo, pLoop, pLoopIteration);
  }

  public static LoopStackInfo of(LoopStackInfo pInfo, boolean pMode) {
    return new LoopStackInfo(pInfo, pMode);
  }

  public LoopStackInfo getPreviousInfo() {
    // copy the previous loop stack to prevent inconsistent modification issue
    if (previousInfo == null) {
      // this case should not happen, but what if it happens?
      return this;
    }
    return new LoopStackInfo(previousInfo);
  }

  public Loop getCurrentLoop() {
    return currentLoop;
  }

  public int getLoopDepth() {
    return loopDepth;
  }

  public int getLoopIteration() {
    return loopIteration;
  }

  public boolean underAbstractMode() {
    return abstractMode;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(previousInfo, currentLoop, loopDepth, loopIteration, abstractMode);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !(obj instanceof LoopStackInfo)) {
      return false;
    }
    LoopStackInfo that = (LoopStackInfo) obj;
    return Objects.equal(previousInfo, that.previousInfo) &&
        Objects.equal(currentLoop, that.currentLoop) &&
        loopDepth == that.loopDepth && loopIteration == that.loopIteration &&
        abstractMode == that.abstractMode;
  }

  public void getIdentifier(@Nonnull List<String> pIdList) {
    if (previousInfo != null) {
      previousInfo.getIdentifier(pIdList);
    }
    pIdList.add(String.valueOf(loopIteration));
  }
}
