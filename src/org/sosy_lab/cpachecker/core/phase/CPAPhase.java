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
package org.sosy_lab.cpachecker.core.phase;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.MainStatistics;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseEmptyResult;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseResult;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseStatus;

import java.util.ArrayList;
import java.util.List;

public abstract class CPAPhase {

  // The ancestors of current phase. Ancestors should be finished prior to current phase
  List<CPAPhase> prevPhases;
  // The successors of current phase. Successors run only after current phase finishes
  private List<CPAPhase> succPhases;
  // The result yielded by current phase
  CPAPhaseResult currResult;

  /**
   * The identifier of current phase
   */
  protected final String id;
  /**
   * The configuration for this phase is not necessary, but is required
   * for satisfying our convention for phases
   */
  protected Configuration config;
  protected final LogManager logger;
  protected final ShutdownManager shutdownManager;
  protected final ShutdownNotifier shutdownNotifier;
  protected final MainStatistics stats;

  /**
   * A flag that indicates whether current CPAPhase is processed
   */
  private boolean isProcessed;

  public CPAPhase(
      String pID, Configuration pConfig, LogManager pLogger, ShutdownManager
      pShutdownManager, ShutdownNotifier pShutdownNotifier, MainStatistics pStats) {
    prevPhases = new ArrayList<>();
    succPhases = new ArrayList<>();
    // by default, we initialize the result as empty result
    currResult = new CPAPhaseEmptyResult();
    isProcessed = false;

    // {@code id} is necessary for identifying different phases
    id = pID;
    config = pConfig;
    logger = pLogger;
    shutdownManager = pShutdownManager;
    shutdownNotifier = pShutdownNotifier;
    stats = pStats;
  }

  public List<CPAPhase> getAncestors() {
    return prevPhases;
  }

  public List<CPAPhase> getSuccessors() {
    return succPhases;
  }

  public void addSuccessor(CPAPhase p) {
    // add phases each other
    this.succPhases.add(p);
    p.prevPhases.add(this);
  }

  public boolean isProcessed() {
    return isProcessed;
  }

  public boolean checkReady() {
    if (isProcessed) {
      // if this phase has been already completed
      return false;
    }
    if (prevPhases.isEmpty()) {
      // if this phase has not been completed and there are no ancestors
      return true;
    }
    // if this phase has not been completed and there exists ancestor(s)
    for (CPAPhase phase : prevPhases) {
      if (!phase.isProcessed) {
        return false;
      }
    }
    return true;
  }

  /**
   * This function defines actions prior to current CPA phase.
   * Possible actions include setup stuffs
   *
   * @return Result of action
   */
  protected abstract CPAPhaseStatus prevAction() throws Exception;

  /**
   * This function defines actions subsequent actions of current CPA phase.
   * Possible actions include cleanups
   *
   * @return Result of action
   */
  protected abstract CPAPhaseStatus postAction() throws Exception;

  /**
   * This function defines what is actually done in current CPA phase.
   *
   * @return Result of action
   */
  protected abstract CPAPhaseStatus runPhase() throws Exception;

  /**
   * We call this function to actually evaluate CPAPhase.
   * NOTE: we do not call the previous by default because developer can call them in different ways
   * e.g. sequential calls, parallel calls
   *
   * @return Result of action
   */
  public CPAPhaseStatus run() throws Exception {
    for (CPAPhase phase : prevPhases) {
      if (!phase.isProcessed()) {
        return CPAPhaseStatus.FAIL;
      }
    }
    CPAPhaseStatus fr;
    fr = CPAPhaseStatus.mergeResult3(prevAction(), runPhase(), postAction());
    this.isProcessed = true;

    return fr;
  }

  public CPAPhaseResult getResult() {
    return currResult;
  }

  public String getIdentifier() {
    return id;
  }

}
