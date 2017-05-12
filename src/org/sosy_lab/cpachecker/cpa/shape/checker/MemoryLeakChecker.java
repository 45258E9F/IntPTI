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
import java.util.Set;

/**
 * A checker for detection of memory leak (CWE 401) issue.
 */
@Options(prefix = "checker.shape.leak")
public class MemoryLeakChecker implements StateChecker<ShapeState>, CheckerWithInstantErrorReport {

  private final List<ErrorReport> errorStore;

  public MemoryLeakChecker(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
    errorStore = new ArrayList<>();
  }

  @Override
  public PL forLanguage() {
    return PL.C;
  }

  @Override
  public Weakness getOrientedWeakness() {
    return Weakness.MEMORY_LEAK;
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
  public List<ARGState> getInverseCriticalStates(
      ARGPath path, ErrorSpot nodeInError) {
    ARGState lastState = path.getLastState();
    return Collections.singletonList(lastState);
  }

  @Override
  public void checkAndRefine(
      ShapeState postState,
      Collection<AbstractState> postOtherStates,
      CFAEdge cfaEdge,
      Collection<ShapeState> newPostStates) throws CPATransferException {
    if (postState.getMemoryLeakStatus()) {
      Set<CFAEdge> leakEdges = postState.getMemoryLeakCFAEdges();
      if (leakEdges.isEmpty()) {
        errorStore.add(MemoryLeakErrorReport.of(null, cfaEdge, this));
      } else {
        for (CFAEdge leakEdge : leakEdges) {
          errorStore.add(MemoryLeakErrorReport.of(null, leakEdge, this));
        }
      }
    }
    newPostStates.add(postState);
  }
}
