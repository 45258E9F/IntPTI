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
package org.sosy_lab.cpachecker.cpa.value.refiner.utils;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisInformation;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.util.refinement.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.GenericEdgeInterpolator;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

@Options(prefix = "cpa.value.interpolation")
public class ValueAnalysisEdgeInterpolator
    extends
    GenericEdgeInterpolator<ValueAnalysisState, ValueAnalysisInformation, ValueAnalysisInterpolant> {

  /**
   * This method acts as the constructor of the class.
   */
  public ValueAnalysisEdgeInterpolator(
      final FeasibilityChecker<ValueAnalysisState> pFeasibilityChecker,
      final StrongestPostOperator<ValueAnalysisState> pStrongestPostOperator,
      final Configuration pConfig,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa
  ) throws InvalidConfigurationException {

    super(
        pStrongestPostOperator,
        pFeasibilityChecker,
        ValueAnalysisInterpolantManager.getInstance(),
        new ValueAnalysisState(pCfa.getMachineModel()),
        ValueAnalysisCPA.class,
        pConfig,
        pShutdownNotifier,
        pCfa);
  }
}
