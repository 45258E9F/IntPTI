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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.checker.CheckerWithInstantErrorReport;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReport;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorSpot;
import org.sosy_lab.cpachecker.core.interfaces.checker.PL;
import org.sosy_lab.cpachecker.core.interfaces.checker.StateChecker;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.shape.ShapeState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.weakness.Weakness;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A general checker for invalid read defects, including (but not limited to):
 * (1) [CWE 476] NPD
 * (2) [CWE 126] Buffer overread
 * (3) [CWE 127] Buffer underread
 * (4) [CWE 416] Use after free
 */
@Options(prefix = "checker.shape.read")
public class InvalidReadChecker implements StateChecker<ShapeState>, CheckerWithInstantErrorReport {

  private final List<ErrorReport> errorStore;

  public InvalidReadChecker(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
    errorStore = new ArrayList<>();
  }

  @Override
  public PL forLanguage() {
    return PL.C;
  }

  @Override
  public Weakness getOrientedWeakness() {
    return Weakness.INVALID_READ;
  }

  @Override
  public Collection<ErrorReport> getErrorReport() {
    return errorStore;
  }

  @Override
  public void resetErrorReport() {
    errorStore.clear();
  }

  @Override
  public Class<? extends AbstractState> getServedStateType() {
    return ShapeState.class;
  }

  @Override
  public void checkAndRefine(
      ShapeState postState,
      Collection<AbstractState> postOtherStates,
      CFAEdge cfaEdge,
      Collection<ShapeState> newPostStates) throws CPATransferException {
    if (postState.getInvalidReadStatus()) {
      for (CExpression faultExp : postState.getInvalidReadExpression()) {
        errorStore.add(InvalidReadErrorReport.of(faultExp, cfaEdge, this));
      }
    }
    newPostStates.add(postState);
  }

  @Override
  public List<ARGState> getInverseCriticalStates(
      ARGPath path, ErrorSpot nodeInError) {
    ARGState lastState = path.getLastState();
    return Collections.singletonList(lastState);
  }
}
