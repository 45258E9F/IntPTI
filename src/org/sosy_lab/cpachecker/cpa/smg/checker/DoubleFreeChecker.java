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
package org.sosy_lab.cpachecker.cpa.smg.checker;

import org.sosy_lab.common.configuration.Configuration;
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
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.errorReport.NPDErrorReport;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.weakness.Weakness;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DoubleFreeChecker implements StateChecker<SMGState>, CheckerWithInstantErrorReport {

  List<ErrorReport> DFs;

  public DoubleFreeChecker(Configuration pConfig) {
    DFs = new ArrayList<>();
  }

  @Override
  public PL forLanguage() {
    return PL.C;
  }

  @Override
  public Weakness getOrientedWeakness() {
    return Weakness.INVALID_FREE;
  }

  @Override
  public Class<? extends AbstractState> getServedStateType() {
    return SMGState.class;
  }

  @Override
  public Collection<ErrorReport> getErrorReport() {
    return DFs;
  }

  @Override
  public void resetErrorReport() {
    DFs.clear();
  }

  @Override
  public List<ARGState> getInverseCriticalStates(ARGPath pPath, ErrorSpot pNodeInError) {
    return pPath.asStatesList();
  }

  @Override
  public void checkAndRefine(
      SMGState pPostState, Collection<AbstractState> pPostOtherStates,
      CFAEdge pCfaEdge, Collection<SMGState> pNewPostStates) throws CPATransferException {
    if (pPostState.checkProperty(SMGState.HAS_INVALID_FREES)) {
      for (CExpression exp : pPostState.getInvalidReadExpression()) {
        DFs.add(NPDErrorReport.create(exp, pCfaEdge, this));
      }
    }
    pNewPostStates.add(pPostState.cleanProperty());

  }

}
