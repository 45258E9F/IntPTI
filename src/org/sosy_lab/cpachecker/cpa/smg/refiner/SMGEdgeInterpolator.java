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

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathPosition;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGStateInformation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.refinement.GenericEdgeInterpolator;
import org.sosy_lab.cpachecker.util.refinement.InterpolantManager;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

import java.util.Deque;
import java.util.Set;

public class SMGEdgeInterpolator
    extends GenericEdgeInterpolator<SMGState, SMGStateInformation, SMGInterpolant> {

  private final SMGFeasibilityChecker checker;
  private final Set<ControlAutomatonCPA> automatons;

  /**
   * the number of interpolations
   */
  private int numberOfInterpolationQueries = 0;

  public SMGEdgeInterpolator(
      StrongestPostOperator<SMGState> pStrongestPostOperator,
      SMGFeasibilityChecker pFeasibilityChecker,
      SMGCPA pSMGCPA,
      Configuration pConfig,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      InterpolantManager<SMGState, SMGInterpolant> pSmgInterpolantManager,
      Set<ControlAutomatonCPA> pAutomatons)
      throws InvalidConfigurationException {
    super(pStrongestPostOperator, pFeasibilityChecker,
        pSmgInterpolantManager,
        pSMGCPA.getInitialState(pCfa.getMainFunction()),
        SMGCPA.class, pConfig,
        pShutdownNotifier, pCfa);
    checker = pFeasibilityChecker;
    automatons = pAutomatons;
  }

  @Override
  public boolean isRemainingPathFeasible(ARGPath pRemainingErrorPath, SMGState pState)
      throws CPAException, InterruptedException {
    numberOfInterpolationQueries++;
    return checker.isFeasible(pRemainingErrorPath, pState, automatons);
  }

  @Override
  public SMGInterpolant deriveInterpolant(
      ARGPath pErrorPath, CFAEdge pCurrentEdge, Deque<SMGState> pCallstack,
      PathPosition pOffset, SMGInterpolant pInputInterpolant)
      throws CPAException, InterruptedException {
    numberOfInterpolationQueries = 0;
    return super
        .deriveInterpolant(pErrorPath, pCurrentEdge, pCallstack, pOffset, pInputInterpolant);
  }

  @Override
  public int getNumberOfInterpolationQueries() {
    return numberOfInterpolationQueries;
  }

}
