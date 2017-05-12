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
package org.sosy_lab.cpachecker.core;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseResult;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

import java.io.PrintStream;

import javax.annotation.Nullable;

/**
 * Class that represents the result of a CPAchecker analysis.
 */
public class CPAcheckerResult implements CPAPhaseResult {

  /**
   * Enum for the possible outcomes of a CPAchecker analysis:
   * - UNKNOWN: analysis did not terminate
   * - FALSE: bug found
   * - TRUE: no bug found
   */
  public static enum Result {
    NOT_YET_STARTED,
    UNKNOWN,
    FALSE,
    TRUE
  }

  private final Result result;

  private final String violatedPropertyDescription;

  private final
  @Nullable
  ReachedSet reached;

  private final
  @Nullable
  Statistics stats;

  private
  @Nullable
  Statistics proofGeneratorStats = null;

  public CPAcheckerResult(
      Result result,
      String violatedPropertyDescription,
      @Nullable ReachedSet reached, @Nullable Statistics stats) {
    this.violatedPropertyDescription = checkNotNull(violatedPropertyDescription);
    this.result = checkNotNull(result);
    this.reached = reached;
    this.stats = stats;
  }

  /**
   * Return the result of the analysis.
   */
  public Result getResult() {
    return result;
  }

  /**
   * Return the final reached set.
   */
  public ReachedSet getReached() {
    return reached;
  }

  public void addProofGeneratorStatistics(Statistics pProofGeneratorStatistics) {
    proofGeneratorStats = pProofGeneratorStatistics;
  }

  public boolean updateCFAStatistics(MainCPAStatistics statistics) {
    return stats instanceof MainCPAStatistics && ((MainCPAStatistics) stats)
        .transferCFAStatistics(statistics);
  }

  /**
   * Write the statistics to a given PrintWriter. Additionally some output files
   * may be written here, if configuration says so.
   */
  public void printStatistics(PrintStream target) {
    if (stats != null) {
      stats.printStatistics(target, result, reached);
    }
    if (proofGeneratorStats != null) {
      proofGeneratorStats.printStatistics(target, result, reached);
    }
  }

  public void printResult(PrintStream out) {
    if (result == Result.NOT_YET_STARTED) {
      return;
    }

    out.println("Verification result: " + getResultString());
  }

  public String getResultString() {
    switch (result) {
      case UNKNOWN:
        return "UNKNOWN, incomplete analysis.";
      case FALSE:
        StringBuilder sb = new StringBuilder();
        sb.append("FALSE. Property violation");
        if (!violatedPropertyDescription.isEmpty()) {
          sb.append(" (").append(violatedPropertyDescription).append(")");
        }
        sb.append(" found by chosen configuration.");
        return sb.toString();
      case TRUE:
        return "TRUE. No property violation found by chosen configuration.";
      default:
        return "UNKNOWN result: " + result;
    }
  }
}
