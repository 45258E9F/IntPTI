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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisStrongestPostOperator;
import org.sosy_lab.cpachecker.util.refinement.GenericPrefixProvider;

public class ValueAnalysisPrefixProvider extends GenericPrefixProvider<ValueAnalysisState> {

  /**
   * This method acts as the constructor of the class.
   *
   * @param pLogger the logger to use
   * @param pCfa    the cfa in use
   */
  public ValueAnalysisPrefixProvider(LogManager pLogger, CFA pCfa, Configuration config)
      throws InvalidConfigurationException {

    super(
        new ValueAnalysisStrongestPostOperator(pLogger, config, pCfa),
        new ValueAnalysisState(pCfa.getMachineModel()),
        pLogger,
        pCfa,
        config,
        ValueAnalysisCPA.class);
  }
}
