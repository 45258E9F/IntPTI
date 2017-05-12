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
package org.sosy_lab.cpachecker.cpa.access;

/**
 * Provide summary, either from:
 * (1) current calculating summary
 * (2) pre-calculated summary
 */
public class AccessSummaryProvider {
  private SummaryManager<AccessAnalysisState> manager;

  private AccessSummaryProvider(SummaryManager<AccessAnalysisState> manager) {
    this.manager = manager;
  }

  public SummaryManager<AccessAnalysisState> provide() {
    return manager;
  }

  /**
   * Create an instance by providing the summary directly
   */
  public static AccessSummaryProvider fromValue(SummaryManager<AccessAnalysisState> manager) {
    return new AccessSummaryProvider(manager);
  }

  /**
   * Create an instance by acquire it from the global shared access summary
   */
  public static AccessSummaryProvider fromGlobalStorage() {
    // TODO: to finish this function later
    throw new RuntimeException("not implemented");
  }
}
