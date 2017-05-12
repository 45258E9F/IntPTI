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
package org.sosy_lab.cpachecker.util.statistics;

import org.sosy_lab.common.time.Timer;


public class StatTimer extends AbstractStatValue {

  private final Timer timer = new Timer();

  public StatTimer(StatKind pMainStatisticKind, String pTitle) {
    super(pMainStatisticKind, pTitle);
  }

  public StatTimer(String pTitle) {
    super(StatKind.SUM, pTitle);
  }

  public void start() {
    timer.start();
  }

  public void stop() {
    timer.stop();
  }

  @Override
  public int getUpdateCount() {
    return timer.getNumberOfIntervals();
  }

  @Override
  public String toString() {
    return timer.toString();
  }

}
