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
package org.sosy_lab.cpachecker.cpa.formulaslicing;

import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

public class InductiveWeakeningStatistics implements Statistics {
  final StatInt noCexIterations = new StatInt(StatKind.AVG,
      "# of CEX iterations");

  final Timer destructiveWeakeningTime = new Timer();
  final Timer cexWeakeningTime = new Timer();

  @Override
  public void printStatistics(
      PrintStream out, Result result, ReachedSet reached) {

    printTimer(out, destructiveWeakeningTime, "Destructive Weakening");
    printTimer(out, cexWeakeningTime, "Counterexample-based Weakening");
    out.printf("# of CEX iterations: %s%n", noCexIterations.toString());
  }

  @Override
  public String getName() {
    return "Inductive Weakening";
  }

  private void printTimer(PrintStream out, Timer t, String name) {
    out.printf("Time spent in %s: %s (Max: %s), (Avg: %s), (#intervals = %s)%n",
        name, t, t.getMaxTime().formatAs(TimeUnit.SECONDS),
        t.getAvgTime().formatAs(TimeUnit.SECONDS),
        t.getNumberOfIntervals());
  }
}
