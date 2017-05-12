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
package org.sosy_lab.cpachecker.cpa.monitor;

import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

class MonitorStatistics implements Statistics {

  private final MonitorCPA mCpa;

  MonitorStatistics(MonitorCPA pCpa) {
    mCpa = pCpa;
  }

  @Override
  public String getName() {
    return "MonitorCPA";
  }

  @Override
  public void printStatistics(
      PrintStream out, Result pResult,
      ReachedSet pReached) {

    MonitorTransferRelation trans = mCpa.getTransferRelation();

    out.println("Max. Post Time:            " + trans.totalTimeOfTransfer.getMaxTime()
        .formatAs(TimeUnit.SECONDS));
    out.println("Avg. Post Time:            " + trans.totalTimeOfTransfer.getAvgTime()
        .formatAs(TimeUnit.SECONDS));
    out.println("Max Post time on a path:   " + TimeSpan.ofMillis(trans.maxTotalTimeForPath)
        .formatAs(TimeUnit.SECONDS));
  }

}
