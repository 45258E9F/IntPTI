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
package org.sosy_lab.cpachecker.util.test;

import static com.google.common.truth.Truth.assertThat;

import org.sosy_lab.cpachecker.core.CPAcheckerResult;

public class TestResults {
  private String log;
  private CPAcheckerResult checkerResult;

  public TestResults(String pLog, CPAcheckerResult pCheckerResult) {
    super();
    log = pLog;
    checkerResult = pCheckerResult;
  }

  public String getLog() {
    return log;
  }

  public CPAcheckerResult getCheckerResult() {
    return checkerResult;
  }

  public void assertIsSafe() {
    assertThat(checkerResult.getResult()).named("verification result")
        .isEqualTo(CPAcheckerResult.Result.TRUE);
  }

  public void assertIsUnsafe() {
    assertThat(checkerResult.getResult()).named("verification result")
        .isEqualTo(CPAcheckerResult.Result.FALSE);
  }

  @Override
  public String toString() {
    return log;
  }
}
