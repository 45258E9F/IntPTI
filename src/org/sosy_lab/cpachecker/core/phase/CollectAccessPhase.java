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
package org.sosy_lab.cpachecker.core.phase;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.MainStatistics;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseStatus;
import org.sosy_lab.cpachecker.cpa.access.AccessAnalysisCPA;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

/**
 * This phase collects access information from the previous Access CPA.
 * Note that the predecessor of this phase must be a {@link SingleAlgorithmRunPhase} instance
 * contains an access CPA, otherwise this phase would fail.
 */
public class CollectAccessPhase extends CPAPhase {

  AccessAnalysisCPA accessCPA;

  public CollectAccessPhase(
      String pIdentifier,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownManager pShutdownManager,
      ShutdownNotifier pShutdownNotifier,
      MainStatistics pStats)
      throws InvalidConfigurationException {
    super(pIdentifier, pConfig, pLogger, pShutdownManager, pShutdownNotifier, pStats);
    accessCPA = null;
  }

  @Override
  protected CPAPhaseStatus prevAction() throws Exception {
    ConfigurableProgramAnalysis cpa = GlobalInfo.getInstance().getCPA().orNull();
    if (cpa == null) {
      throw new InvalidConfigurationException("Access CPA should be (immediate) predecessor");
    }
    accessCPA = CPAs.retrieveCPA(cpa, AccessAnalysisCPA.class);
    if (accessCPA == null) {
      throw new InvalidConfigurationException("Access CPA should be (immediate) predecessor");
    }
    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus postAction() throws Exception {
    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus runPhase() throws Exception {
    // TODO: we extract access summary for later query
    return CPAPhaseStatus.SUCCESS;
  }

}
