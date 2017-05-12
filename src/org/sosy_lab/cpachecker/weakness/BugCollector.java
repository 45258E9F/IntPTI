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
package org.sosy_lab.cpachecker.weakness;

import com.google.common.base.Equivalence;
import com.google.common.base.Equivalence.Wrapper;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.checker.DefaultTracedErrorReport;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReport;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReportWithTrace;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorSpot;
import org.sosy_lab.cpachecker.core.interfaces.checker.util.ErrorReports;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.util.report.ReportUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.edu.thu.tsmart.tool.bd.report.FaultResult;
import cn.edu.thu.tsmart.tool.bd.report.Report;
import cn.edu.thu.tsmart.tool.bd.report.WeaknessInfo;


/**
 * A collector interface for managing detected bugs (error reports).
 * NOTE: formerly we intend to use ErrorCollector, however this name has been taken by JUnit.
 */
public final class BugCollector {

  private final List<ErrorReport> errorList;

  private long counter = 0;

  private BugCollector() {
    errorList = Lists.newArrayList();
  }

  public static BugCollector createInstance() {
    return new BugCollector();
  }

  public void addErrorRecord(ErrorReport error) {
    errorList.add(error);
  }

  public static Map<String, Integer> errorIndexes = new HashMap<>();

  /**
   * The export method for log output.
   */
  public BugSummary exportForLog() {

    // STEP 1: post-processing of error reports
    Multimap<Wrapper<ErrorReport>, Wrapper<ErrorReport>> errorMap = HashMultimap.create();
    fromListToMap(errorMap);

    // STEP 2: summarize alerts
    Set<Wrapper<ErrorReport>> errorSet = errorMap.keySet();
    BugSummary summary = new BugSummary();
    for (Wrapper<ErrorReport> alert : errorSet) {
      ErrorReport report = alert.get();
      if (report == null) {
        continue;
      }
      summary.insertEntry(report);
    }

    return summary;
  }

  /**
   * The export method for error report.
   */
  public Report exportForReport() {

    Multimap<Wrapper<ErrorReport>, Wrapper<ErrorReport>> errorMap = HashMultimap.create();
    fromListToMap(errorMap);

    List<FaultResult> faultList = new ArrayList<>();
    List<ErrorReport> errors = new ArrayList<>();
    for (Wrapper<ErrorReport> error : errorMap.keySet()) {
      Collection<Wrapper<ErrorReport>> faults = errorMap.get(error);
      for (Wrapper<ErrorReport> shadowError : faults) {
        ErrorReport report = shadowError.get();
        FaultResult fault = ReportUtil.convert(report);
        faultList.add(fault);
      }
      errors.add(error.get());
    }

    //for debug
    printErrorTraces(errors);

    List<WeaknessInfo> weaknessList = new ArrayList<>();
    for (Weakness weakness : Weakness.values()) {
      weaknessList.add(new WeaknessInfo(weakness.toString()));
    }

    String projectPath = System.getProperty("user.dir");
    // for now, the generated report does not contain runtime info.
    return new Report(weaknessList, projectPath, 0, 0, faultList);
  }

  public void printErrorTraces(List<ErrorReport> errors) {

    String projectPath = System.getProperty("user.dir");
    String errorTracePath = projectPath + "/ErrorTraces";
    File errorDir = new File(errorTracePath);
    if (errorIndexes.isEmpty() && errorDir.isDirectory() && errorDir.exists()) {
      String[] children = errorDir.list();
      for (int i = 0; i < children.length; i++) {
        File child = new File(errorTracePath + "/" + children[i]);
        child.delete();
      }
      errorDir.delete();
    }

    try {
      for (ErrorReport error : errors) {
        Optional<CFAEdge> errorEdge = error.getErrorSpot().getCFAEdge();
        String errorFileName = "";
        if (errorEdge.isPresent()) {
          String errorFilePath = errorEdge.get().getFileLocation().getFileName();
          File temp = new File(errorFilePath);
          errorFileName = temp.getName();
        }
        if (errorIndexes.containsKey(errorFileName)) {
          int temp = errorIndexes.get(errorFileName);
          errorIndexes.put(errorFileName, temp + 1);
          errorFileName = errorFileName + "-" + temp;
        } else {
          errorIndexes.put(errorFileName, 2);
          errorFileName = errorFileName + "-1";
        }
        File file = new File(errorTracePath, errorFileName + ".txt");
        Files.createParentDirs(file);
        FileWriter writer = new FileWriter(file, false);
        Weakness errorType = error.getWeakness();
        writer.write(errorType.getWeaknessName() + "\n\n");
        ARGPath errorTrace;
        String errorCode = "";
        if (error instanceof DefaultTracedErrorReport) {
          errorTrace = ((DefaultTracedErrorReport) error).getErrorTrace();
          List<CFAEdge> edges = errorTrace.getFullPath();
          for (CFAEdge edge : edges) {
            String code = edge.getCode();
            if (code.length() > 0 && code.charAt(0) != '#') {
              errorCode = errorCode + edge.getFileLocation().getStartingLineNumber() + "      " +
                  code + "\n";
            }
          }
        } else {
          Optional<CFAEdge> edge = error.getErrorSpot().getCFAEdge();
          if (edge.isPresent()) {
            String code = edge.get().getCode();
            if (code.length() > 0 && code.charAt(0) != '#') {
              errorCode = edge.get().getFileLocation().getStartingLineNumber() + "      " +
                  code + "\n";
            }
          }
        }
        writer.write(errorCode + "\n\n\n");
        writer.close();
      }
    } catch (IOException pE) {
      pE.printStackTrace();
    }

  }

  /**
   * Contract: this method should be called instantly after the export method(s) is(are) called.
   */
  public void resetBugCollector() {
    Multimap<Wrapper<ErrorReport>, Wrapper<ErrorReport>> errorMap = HashMultimap.create();
    fromListToMap(errorMap);
    counter += errorMap.keySet().size();
    errorList.clear();
  }

  /**
   * This function transforms error list to mapping.
   * Some additional work is required:
   * (1) merging equivalent error reports
   * (2) classify error reports with respect to ARG state
   */
  private void fromListToMap(Multimap<Wrapper<ErrorReport>, Wrapper<ErrorReport>> newErrorMap) {
    for (ErrorReport error : errorList) {
      Wrapper<ErrorReport> keyWrapper = LOOSE_ERROR_EQUIVALENCE.wrap(error);
      Wrapper<ErrorReport> valWrapper = STRICT_ERROR_EQUIVALENCE.wrap(error);
      newErrorMap.put(keyWrapper, valWrapper);
    }
  }

  public long getBugSize() {
    return counter;
  }

  private Equivalence<ErrorReport> STRICT_ERROR_EQUIVALENCE = new Equivalence<ErrorReport>() {
    @Override
    protected boolean doEquivalent(
        ErrorReport pErrorReport, ErrorReport pT1) {
      return ErrorReports.equals(pErrorReport, pT1);
    }

    @Override
    protected int doHash(ErrorReport pErrorReport) {
      if (pErrorReport instanceof ErrorReportWithTrace) {
        ErrorReportWithTrace tracedError = (ErrorReportWithTrace) pErrorReport;
        return Objects.hashCode(tracedError.getWeakness(), tracedError.getErrorSpot(),
            tracedError.getErrorTrace());
      } else {
        return Objects.hashCode(pErrorReport.getWeakness(), pErrorReport.getErrorSpot());
      }
    }
  };

  private Equivalence<ErrorReport> LOOSE_ERROR_EQUIVALENCE = new Equivalence<ErrorReport>() {
    @Override
    protected boolean doEquivalent(
        ErrorReport pErrorReport, ErrorReport pT1) {
      return ErrorReports.similarize(pErrorReport, pT1);
    }

    @Override
    protected int doHash(ErrorReport pErrorReport) {
      ErrorSpot spot = pErrorReport.getErrorSpot();
      return Objects
          .hashCode(pErrorReport.getWeakness(), spot.getASTNode(), spot.getFileLocation());
    }
  };

}
