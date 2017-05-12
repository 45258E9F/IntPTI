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
package org.sosy_lab.cpachecker.cpa.coverage;

import org.sosy_lab.cpachecker.cpa.coverage.CoverageData.FileCoverage;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;

import java.io.PrintStream;
import java.util.Map;

class CoverageReportStdoutSummary {

  public void write(CoverageData pData, PrintStream pStdOut) {

    long numTotalConditions = 0;
    long numTotalFunctions = 0;
    long numTotalLines = 0;

    long numVisitedConditions = 0;
    long numVisitedFunctions = 0;
    long numVisitedLines = 0;

    Map<String, FileCoverage> infos = pData.getInfosPerFile();

    for (FileCoverage info : infos.values()) {
      numTotalFunctions = +info.allFunctions.size();
      numVisitedFunctions = +info.visitedFunctions.size();

      numTotalConditions = +info.allAssumes.size();
      numVisitedConditions = +info.visitedAssumes.size();

      numTotalLines = +info.allLines.size();

      for (Integer line : info.allLines) {
        if (info.visitedLines.get(line)) {
          numVisitedLines += 1;
        }
      }
    }

    if (numTotalFunctions > 0) {
      final double functionCoverage = numVisitedFunctions / (double) numTotalFunctions;
      StatisticsUtils
          .write(pStdOut, 1, 25, "Function coverage", String.format("%.3f", functionCoverage));
    }

    if (numTotalLines > 0) {
      final double lineCoverage = numVisitedLines / (double) numTotalLines;
      StatisticsUtils.write(pStdOut, 1, 25, "Visited lines", numVisitedLines);
      StatisticsUtils.write(pStdOut, 1, 25, "Total lines", numTotalLines);
      StatisticsUtils.write(pStdOut, 1, 25, "Line coverage", String.format("%.3f", lineCoverage));
    }

    if (numTotalConditions > 0) {
      final double conditionCoverage = numVisitedConditions / (double) numTotalConditions;
      StatisticsUtils.write(pStdOut, 1, 25, "Visited conditions", numVisitedConditions);
      StatisticsUtils.write(pStdOut, 1, 25, "Total conditions", numTotalConditions);
      StatisticsUtils
          .write(pStdOut, 1, 25, "Condition coverage", String.format("%.3f", conditionCoverage));
    }

    pStdOut.println();

  }

}
