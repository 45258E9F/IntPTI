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

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

/**
 * Statistics for formula slicing.
 */
class FormulaSlicingStatistics implements Statistics {
  final Timer propagation = new Timer();
  final Timer reachability = new Timer();
  final Timer inductiveWeakening = new Timer();
  final Timer deadVarElimination = new Timer();
  final Timer semiCnfConversion = new Timer();

  @Override
  public void printStatistics(
      PrintStream out,
      Result result,
      ReachedSet reached) {
    printTimer(out, propagation, "propagating formulas");
    printTimer(out, reachability, "checking reachability");
    printTimer(out, inductiveWeakening, "inductive weakening");
    printTimer(out, deadVarElimination, "eliminating dead variables");
    printTimer(out, semiCnfConversion, "converting to SemiCNF");
  }

  @Override
  public String getName() {
    return "Formula Slicing Manager";
  }

  private void printTimer(PrintStream out, Timer t, String name) {
    out.printf("Time spent in %s: %s (Max: %s), (Avg: %s), (#intervals = %s)%n",
        name,
        t.getSumTime().formatAs(TimeUnit.SECONDS),
        t.getMaxTime().formatAs(TimeUnit.SECONDS),
        t.getAvgTime().formatAs(TimeUnit.SECONDS),
        t.getNumberOfIntervals());
  }
}
