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
 * A general checker for invalid write defects, including:
 * (1) [CWE 124] buffer underwrite
 * (2) [CWE 121] stack-based buffer overflow
 * (3) [CWE 122] heap-based buffer overflow
 */
@Options(prefix = "checker.shape.write")
public class InvalidWriteChecker
    implements StateChecker<ShapeState>, CheckerWithInstantErrorReport {

  private final List<ErrorReport> errorStore;

  public InvalidWriteChecker(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
    errorStore = new ArrayList<>();
  }

  @Override
  public PL forLanguage() {
    return PL.C;
  }

  @Override
  public Weakness getOrientedWeakness() {
    return Weakness.INVALID_WRITE;
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
    if (postState.getInvalidWriteStatus()) {
      errorStore.add(InvalidWriteErrorReport.of(null, cfaEdge, this));
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
