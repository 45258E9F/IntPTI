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
package org.sosy_lab.cpachecker.cpa.livevar.checker;

import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReport;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorSpot;
import org.sosy_lab.cpachecker.cpa.livevar.LiveVariablesState;
import org.sosy_lab.cpachecker.weakness.Weakness;

public class UnusedVarsErrorReport implements ErrorReport {

  private final ErrorSpot targetSpot;

  protected UnusedVarsErrorReport(CAstNode node, CFAEdge edge) {
    targetSpot = new ErrorSpot(node, edge);
  }

  @Override
  public ErrorSpot getErrorSpot() {
    return targetSpot;
  }

  @Override
  public Weakness getWeakness() {
    return Weakness.UNUSED_VARIABLE;
  }

  @Override
  public Class<? extends AbstractState> getSourceStateClass() {
    return LiveVariablesState.class;
  }

}
