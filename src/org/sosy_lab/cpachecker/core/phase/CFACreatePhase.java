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

import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.MainStatistics;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseStatus;
import org.sosy_lab.cpachecker.util.globalinfo.BasicIOManager;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * This is usually the initial phase for all kinds of analysis.
 * This is phase has empty phase result. The created CFA is stored
 * directly in Global Info.
 */
@Options(prefix = "phase.cfaCreation")
public class CFACreatePhase extends CPAPhase implements StatisticsProvider {

  private final String programDenotation;

  @SuppressWarnings("unused")
  public CFACreatePhase(
      String pIdentifier,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownManager pShutdownManager,
      ShutdownNotifier pShutdownNotifier,
      MainStatistics pStats)
      throws InvalidConfigurationException {
    // this phase has its own statistics object
    super(pIdentifier, pConfig, pLogger, pShutdownManager, pShutdownNotifier, pStats);
    pConfig.inject(this);
    programDenotation = GlobalInfo.getInstance().getIoManager().getProgramNames();

    // setup a sub-directory for this phase and re-setup paths for file type conversion
    BasicIOManager IOManager = GlobalInfo.getInstance().getIoManager();
    String rootOutputDirectory = BasicIOManager.concatPath(IOManager.getBasicOutputDirectory(), id);
    config = BasicIOManager.setupPaths(rootOutputDirectory, config, IOManager.getSecureMode());
  }

  @Override
  protected CPAPhaseStatus prevAction() throws Exception {
    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus postAction() throws Exception {
    currResult = stats;
    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus runPhase() throws Exception {
    Stopwatch watch = Stopwatch.createStarted();
    System.out.println("Parsing files and creating CFA...");

    CFA cfa = parse(programDenotation, stats);
    GlobalInfo.getInstance().storeCFA(cfa);

    System.out.println(
        String.format("PARSE: %.3f", watch.elapsed(TimeUnit.MILLISECONDS) / 1000.0));
    return CPAPhaseStatus.SUCCESS;
  }

  private CFA parse(String pProgramFiles, MainStatistics pStats)
      throws Exception {
    // in configuration of {@link CFACreationPhase}, it is necessary to specify parameters for
    // CFA creator
    CFACreator creator = new CFACreator(config, logger, shutdownNotifier);
    pStats.setCFACreator(creator);
    Splitter commaSplitter = Splitter.on(',').omitEmptyStrings().trimResults();
    CFA cfa = creator.parseFileAndCreateCFA(commaSplitter.splitToList(pProgramFiles));
    pStats.setCFA(cfa);
    return cfa;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(stats);
  }
}
