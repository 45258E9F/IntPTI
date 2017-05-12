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

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReport;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorSpot;

import java.util.ArrayList;
import java.util.List;

public final class BugSummary {

  private long bugSize;
  private List<BugEntry> bugEntryList;

  public BugSummary() {
    bugSize = 0;
    bugEntryList = new ArrayList<>();
  }

  private final class BugEntry {
    private final String weaknessName;
    private final String operation;
    private final String functionName;
    private final FileLocation location;

    BugEntry(
        String pWeaknessName, String pOperation, String pFunctionName, FileLocation
        pLocation) {
      weaknessName = pWeaknessName;
      operation = pOperation;
      functionName = pFunctionName;
      location = pLocation;
    }

    String getWeaknessName() {
      return weaknessName;
    }

    String getOperation() {
      return operation;
    }

    String getFunctionName() {
      return functionName;
    }

    FileLocation getLocation() {
      return location;
    }
  }

  public void insertEntry(ErrorReport report) {
    String defectName = report.getWeakness().toString();
    ErrorSpot spot = report.getErrorSpot();
    String strOperation;
    Optional<CAstNode> astNode = spot.getASTNode();
    Optional<CFAEdge> edge = spot.getCFAEdge();
    if (astNode.isPresent()) {
      CAstNode ast = astNode.get();
      strOperation = ast.toASTString();
    } else if (edge.isPresent()) {
      CFAEdge cfaEdge = edge.get();
      strOperation = cfaEdge.toString();
    } else {
      strOperation = "unavailable info.";
    }
    String funcName = spot.getFunctionName();
    FileLocation loc = spot.getFileLocation();
    BugEntry newEntry = new BugEntry(defectName, strOperation, funcName, loc);
    bugEntryList.add(newEntry);
    bugSize++;
  }

  public long getBugSize() {
    return bugSize;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Total number of alerts: ");
    builder.append(bugSize);
    builder.append("\n\n");
    for (BugEntry entry : bugEntryList) {
      builder.append("<\n");
      builder.append("Weakness: ");
      builder.append(entry.getWeaknessName());
      builder.append("\n");
      builder.append("Operation: ");
      builder.append(entry.getOperation());
      builder.append("\n");
      builder.append("Function: ");
      builder.append(entry.getFunctionName());
      builder.append("\n");
      builder.append("Location: ");
      builder.append(entry.getLocation());
      builder.append("\n>\n\n");
    }
    return builder.toString();
  }
}