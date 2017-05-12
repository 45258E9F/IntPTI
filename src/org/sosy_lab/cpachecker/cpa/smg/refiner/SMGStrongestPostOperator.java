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
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

import java.util.Collection;
import java.util.Deque;


public class SMGStrongestPostOperator implements StrongestPostOperator<SMGState> {

  private final SMGTransferRelation transfer;

  public SMGStrongestPostOperator(LogManager pLogger, Configuration pBuild, CFA pCfa)
      throws InvalidConfigurationException {
    transfer = SMGTransferRelation
        .createTransferRelationForRefinement(pBuild, pLogger, pCfa.getMachineModel());
  }

  @Override
  public Optional<SMGState> getStrongestPost(
      SMGState pOrigin,
      Precision pPrecision,
      CFAEdge pOperation)
      throws CPAException, InterruptedException {


    final Collection<? extends AbstractState> successors =
        transfer.getAbstractSuccessorsForEdge(pOrigin, Lists.<AbstractState>newArrayList(),
            pPrecision, pOperation);

    if (successors.isEmpty()) {
      return Optional.absent();

    } else {
      return Optional.of((SMGState) Iterables.getOnlyElement(successors));
    }
  }

  @Override
  public SMGState handleFunctionCall(SMGState pState, CFAEdge pEdge, Deque<SMGState> pCallstack) {
    return pState;
  }

  @Override
  public SMGState handleFunctionReturn(SMGState pNext, CFAEdge pEdge, Deque<SMGState> pCallstack) {
    // TODO investigate scoping?
    return pNext;
  }

  @Override
  public SMGState performAbstraction(
      SMGState pNext,
      CFANode pCurrNode,
      ARGPath pErrorPath,
      Precision pPrecision) {
    // TODO Investigate abstraction
    return pNext;
  }

}
