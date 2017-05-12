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
package org.sosy_lab.cpachecker.cpa.automaton;

import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;

import java.io.PrintStream;

class AutomatonStatistics extends AbstractStatistics {

  private final ControlAutomatonCPA mCpa;

  public AutomatonStatistics(ControlAutomatonCPA pCpa) {
    mCpa = pCpa;
  }

  @Override
  public String getName() {
    return "AutomatonAnalysis (" + mCpa.getAutomaton().getName() + ")";
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
    AutomatonTransferRelation trans = mCpa.getTransferRelation();

    put(out, 0, "Number of states", mCpa.getAutomaton().getNumberOfStates());
    put(out, 0, "Total time for successor computation", trans.totalPostTime);

    if (trans.totalPostTime.getSumTime().compareTo(TimeSpan.ofMillis(500)) >= 0) {
      // normally automaton is very fast, and time measurements are very imprecise
      // so don't care about very small times
      put(out, 1, "Time for transition matches", trans.matchTime);
      put(out, 1, "Time for transition assertions", trans.assertionsTime);
      put(out, 1, "Time for transition actions", trans.actionTime);
    }

    if (trans.totalStrengthenTime.getNumberOfIntervals() > 0) {
      put(out, 0, "Total time for strengthen operator", trans.totalStrengthenTime);
    }

    int stateBranchings = trans.automatonSuccessors.getValueCount()
        - trans.automatonSuccessors.getTimesWithValue(0)
        - trans.automatonSuccessors.getTimesWithValue(1);
    put(out, 0, "Automaton transfers with branching", stateBranchings);
    put(out, 0, "Automaton transfer successors", trans.automatonSuccessors);
  }
}
