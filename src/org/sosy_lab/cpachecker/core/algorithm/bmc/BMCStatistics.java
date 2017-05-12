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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

import java.io.PrintStream;

public class BMCStatistics implements Statistics {

  final Timer satCheck = new Timer();
  final Timer errorPathCreation = new Timer();
  final Timer assertionsCheck = new Timer();

  final Timer inductionPreparation = new Timer();
  final Timer inductionCheck = new Timer();
  private int inductionCutPoints = 0;

  @Override
  public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
    if (satCheck.getNumberOfIntervals() > 0) {
      out.println("Time for final sat check:            " + satCheck);
    }
    if (errorPathCreation.getNumberOfIntervals() > 0) {
      out.println("Time for error path creation:        " + errorPathCreation);
    }
    if (assertionsCheck.getNumberOfIntervals() > 0) {
      out.println("Time for bounding assertions check:  " + assertionsCheck);
    }
    if (inductionCheck.getNumberOfIntervals() > 0) {
      out.println("Number of cut points for induction:  " + inductionCutPoints);
      out.println("Time for induction formula creation: " + inductionPreparation);
      out.println("Time for induction check:            " + inductionCheck);
    }
  }

  @Override
  public String getName() {
    return "BMC algorithm";
  }
}