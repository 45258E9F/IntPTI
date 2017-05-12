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
package org.sosy_lab.cpachecker.core.interfaces.checker;

import com.google.common.collect.Lists;

import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import java.util.List;

/**
 * A default abstract class for error report with trace
 */
public abstract class DefaultTracedErrorReport implements ErrorReportWithTrace {

  protected final ErrorSpot targetSpot;
  protected final CheckerWithInstantErrorReport internalChecker;
  protected ARGPath errorTrace;
  protected List<ARGState> criticalStates;

  protected DefaultTracedErrorReport(
      CAstNode node, CFAEdge edge,
      CheckerWithInstantErrorReport pChecker) {
    targetSpot = new ErrorSpot(node, edge);
    internalChecker = pChecker;
    errorTrace = null;
    criticalStates = Lists.newArrayList();
  }

  @Override
  public List<ARGState> getCriticalErrorStates() {
    return criticalStates;
  }

  @Override
  public ARGPath getErrorTrace() {
    return errorTrace;
  }

  @Override
  public CheckerWithInstantErrorReport getChecker() {
    return internalChecker;
  }

  @Override
  public ErrorSpot getErrorSpot() {
    return targetSpot;
  }

  @Override
  public void updateCriticalStates(List<ARGState> pCriticalStates) {
    criticalStates.clear();
    criticalStates.addAll(pCriticalStates);
  }

  @Override
  public void updateErrorTrace(ARGPath pErrorTrace) {
    errorTrace = pErrorTrace;
  }
}
