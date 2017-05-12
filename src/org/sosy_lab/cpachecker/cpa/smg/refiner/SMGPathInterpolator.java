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
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.util.refinement.GenericPathInterpolator;
import org.sosy_lab.cpachecker.util.refinement.GenericPrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.InterpolantManager;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

import java.util.Set;


public class SMGPathInterpolator extends GenericPathInterpolator<SMGState, SMGInterpolant> {

  public SMGPathInterpolator(
      SMGFeasibilityChecker pChecker,
      StrongestPostOperator<SMGState> pStrongestPostOp,
      GenericPrefixProvider<SMGState> pPrefixProvider,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      SMGCPA pSMGCPA,
      InterpolantManager<SMGState, SMGInterpolant> pSMGInterpolantManager,
      Set<ControlAutomatonCPA> pAutomatons) throws InvalidConfigurationException {

    super(
        new SMGEdgeInterpolator(pStrongestPostOp,
            pChecker,
            pSMGCPA,
            pConfig,
            pShutdownNotifier,
            pCfa,
            pSMGInterpolantManager,
            pAutomatons),
        pChecker,
        pPrefixProvider,
        pSMGInterpolantManager,
        pConfig,
        pLogger,
        pShutdownNotifier,
        pCfa);
  }
}
