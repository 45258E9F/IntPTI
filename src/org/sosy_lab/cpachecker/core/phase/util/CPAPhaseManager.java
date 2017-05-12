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
package org.sosy_lab.cpachecker.core.phase.util;

import static org.sosy_lab.cpachecker.core.phase.util.CPAPhaseManager.ExecutionType.SEQUENTIAL;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.MainStatistics;
import org.sosy_lab.cpachecker.core.phase.CPAPhase;
import org.sosy_lab.cpachecker.core.phase.config.PhaseConfigManager;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseStatus;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * This class manages multiple CPA phases used in analysis. Necessary members include: (1) CPA phase
 * work-flow graph, (2) executionStrategy (sequential/parallel), (3) execute() method to run phases
 * from one (specified?) entry
 */
@Options(prefix = "phase.manager")
public class CPAPhaseManager {

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownManager shutdownManager;
  private final ShutdownNotifier shutdownNotifier;
  private final MainStatistics stats;

  private List<CPAPhase> phases;

  private CPAPhaseExecStrategy execStg;

  public enum ExecutionType {
    /**
     * Every phase is executed in sequential
     */
    SEQUENTIAL,
    /**
     * Phases are executed in parallel if possible
     */
    PARALLEL
    // TODO: other execution strategies
  }

  @Option(secure = true, name = "config", description = "the configuration of phase manager")
  private String managerConfig = "config/phase/default.config";

  @Option(secure = true, description = "the execution strategy of phases")
  private ExecutionType executionType = SEQUENTIAL;

  public CPAPhaseManager(
      Configuration pConfig, LogManager pLogger, ShutdownManager
      pShutdownManager, ShutdownNotifier pShutdownNotifier, MainStatistics pStats)
      throws InvalidConfigurationException {
    config = pConfig;
    config.inject(this);
    logger = pLogger;
    shutdownManager = pShutdownManager;
    shutdownNotifier = pShutdownNotifier;
    stats = pStats;
    execStg = createExecutionStrategy(executionType);
  }

  public void initialize() throws IOException, InvalidConfigurationException {
    // check the validity of sub-configuration file
    File configFile = new File(managerConfig);
    if (!configFile.exists()) {
      throw new InvalidConfigurationException("Phase configuration file not exist");
    }
    PhaseConfigManager manager = new PhaseConfigManager(configFile.getAbsolutePath(),
        config, logger, shutdownManager, shutdownNotifier, stats);
    // create phases according to configuration
    manager.initialize();
    phases = manager.createPhases();
  }

  public void execute() throws Exception {
    CPAPhaseStatus status = execStg.exec(phases);
    assert (status != null);
  }

  private CPAPhaseExecStrategy createExecutionStrategy(ExecutionType type)
      throws InvalidConfigurationException {
    switch (type) {
      case SEQUENTIAL:
        return new SequentialExecStrategy();
      case PARALLEL:
        throw new InvalidConfigurationException("Unimplemented method");
      default:
        throw new InvalidConfigurationException("Invalid execution strategy chosen");
    }
  }

}
