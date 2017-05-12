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
package org.sosy_lab.cpachecker.cpa.shape.checker;

import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.checker.CheckerWithInstantErrorReport;
import org.sosy_lab.cpachecker.core.interfaces.checker.DefaultTracedErrorReport;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReportWithTrace;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.weakness.Weakness;

import javax.annotation.Nullable;

public class MemoryLeakErrorReport extends DefaultTracedErrorReport
    implements ErrorReportWithTrace {

  private MemoryLeakErrorReport(
      @Nullable CAstNode pNode, @Nullable CFAEdge pEdge,
      CheckerWithInstantErrorReport pChecker) {
    super(pNode, pEdge, pChecker);
  }

  public static MemoryLeakErrorReport of(
      @Nullable CAstNode pNode, @Nullable CFAEdge pEdge,
      CheckerWithInstantErrorReport pChecker) {
    return new MemoryLeakErrorReport(pNode, pEdge, pChecker);
  }

  @Override
  public Weakness getWeakness() {
    return Weakness.MEMORY_LEAK;
  }

  @Override
  public Class<? extends AbstractState> getSourceStateClass() {
    return ShapeState.class;
  }
}
