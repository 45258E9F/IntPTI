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
package org.sosy_lab.cpachecker.util.coverage;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class FileCoverageInformation {

  static class FunctionInfo {
    final String name;
    final int firstLine;
    final int lastLine;

    FunctionInfo(String pName, int pFirstLine, int pLastLine) {
      name = pName;
      firstLine = pFirstLine;
      lastLine = pLastLine;
    }
  }

  final Map<Integer, Integer> visitedLines = new HashMap<>();
  final Set<Integer> allLines = new HashSet<>();
  final Map<String, Integer> visitedFunctions = new HashMap<>();
  final Set<FunctionInfo> allFunctions = new HashSet<>();
  final Set<AssumeEdge> allAssumes = new HashSet<>();
  final Set<AssumeEdge> visitedAssumes = new HashSet<>();

  public void addVisitedAssume(AssumeEdge pEdge) {
    visitedAssumes.add(pEdge);
  }

  public void addExistingAssume(AssumeEdge pEdge) {
    allAssumes.add(pEdge);
  }

  public void addVisitedFunction(String pName, int pCount) {
    if (visitedFunctions.containsKey(pName)) {
      visitedFunctions.put(pName, visitedFunctions.get(pName) + pCount);
    } else {
      visitedFunctions.put(pName, pCount);
    }
  }

  public void addExistingFunction(String pName, int pFirstLine, int pLastLine) {
    allFunctions.add(new FunctionInfo(pName, pFirstLine, pLastLine));
  }

  public void addVisitedLine(int pLine) {
    checkArgument(pLine > 0);
    if (visitedLines.containsKey(pLine)) {
      visitedLines.put(pLine, visitedLines.get(pLine) + 1);
    } else {
      visitedLines.put(pLine, 1);
    }
  }

  public int getVisitedLine(int pLine) {
    checkArgument(pLine > 0);
    return visitedLines.containsKey(pLine) ? visitedLines.get(pLine) : 0;
  }

  public void addExistingLine(int pLine) {
    checkArgument(pLine > 0);
    allLines.add(pLine);
  }

}
