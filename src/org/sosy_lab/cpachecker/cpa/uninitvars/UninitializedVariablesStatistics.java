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
package org.sosy_lab.cpachecker.cpa.uninitvars;

import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Statistics for UninitializedVariablesCPA.
 * Displays warnings about all uninitialized variables found.
 */
public class UninitializedVariablesStatistics implements Statistics {

  private boolean printWarnings;

  public UninitializedVariablesStatistics(String printWarnings) {
    super();
    this.printWarnings = Boolean.parseBoolean(printWarnings);
  }

  @Override
  public String getName() {
    return "UninitializedVariablesCPA";
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {

    int noOfWarnings = 0;

    if (printWarnings) {

      Set<Pair<Integer, String>> warningsDisplayed = new HashSet<>();
      Iterable<UninitializedVariablesState> projectedReached =
          AbstractStates.projectToType(pReached, UninitializedVariablesState.class);

      //find all UninitializedVariablesElements and get their warnings
      for (UninitializedVariablesState uninitElement : projectedReached) {

        Collection<Triple<Integer, String, String>> warnings = uninitElement.getWarnings();
        //warnings are identified by line number and variable name
        Pair<Integer, String> warningIndex;
        for (Triple<Integer, String, String> warning : warnings) {
          //check if a warning has already been displayed
          warningIndex = Pair.of(warning.getFirst(), warning.getSecond());
          if (!warningsDisplayed.contains(warningIndex)) {
            warningsDisplayed.add(warningIndex);
            pOut.println(warning.getThird());
            noOfWarnings++;
          }
        }

      }
      if (warningsDisplayed.isEmpty()) {
        pOut.println("No uninitialized variables found");
      } else {
        pOut.println("No of uninitialized vars : " + noOfWarnings);
      }
    } else {
      pOut.println("Output deactivated by configuration option");
    }
  }
}
