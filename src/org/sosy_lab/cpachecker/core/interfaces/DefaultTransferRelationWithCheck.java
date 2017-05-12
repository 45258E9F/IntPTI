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
package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.checker.CheckerManager;
import org.sosy_lab.cpachecker.core.interfaces.checker.ErrorReport;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A default implementation of a transfer relation with check.
 * This implementation use checker manager to check states and perform no expression checks. A
 * typical state check task can directly uses this abstract class.
 *
 * @param <T> concrete class of abstract state
 */
public abstract class DefaultTransferRelationWithCheck<T extends AbstractState>
    implements TransferRelationWithCheck {

  CheckerManager<T> checkerManager;

  protected DefaultTransferRelationWithCheck(Configuration pConfig, Class<T> classT)
      throws InvalidConfigurationException {
    checkerManager = new CheckerManager<>(pConfig, classT);
  }

  @Override
  public Collection<ErrorReport> getErrorReports() {
    return checkerManager.getErrorReportInChecker();
  }

  @Override
  public void resetErrorReports() {
    checkerManager.resetErrorReportInChecker();
  }

  @Override
  public Collection<ErrorReport> dumpErrorsAfterAnalysis() {
    return checkerManager.dumpErrors();
  }

  @Override
  public Collection<? extends AbstractState> checkAndRefineExpression(
      AbstractState preState,
      List<AbstractState> preOtherState,
      Precision precision,
      CFAEdge cfaEdge) throws CPATransferException, InterruptedException {
    return Collections.singleton(preState);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Collection<? extends AbstractState> checkAndRefineState(
      AbstractState postState,
      List<AbstractState> postOtherStates,
      Precision precision,
      CFAEdge cfaEdge) throws CPATransferException, InterruptedException {

    T coreState = (T) postState;
    Collection<T> resultStates = new ArrayList<>();
    checkerManager.checkState(coreState, postOtherStates, cfaEdge, resultStates);
    return resultStates;
  }

}
