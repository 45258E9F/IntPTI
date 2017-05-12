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

import static org.sosy_lab.common.DuplicateOutputStream.mergeStreams;
import static org.sosy_lab.cpachecker.core.phase.BasicAnalysisPhase.AnalysisKind.SINGLE;

import com.google.common.io.Closer;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cmdline.ForceTerminationOnShutdown;
import org.sosy_lab.cpachecker.cmdline.ShutdownHook;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.HierarchicalCPAStatistics;
import org.sosy_lab.cpachecker.core.MainCPAStatistics;
import org.sosy_lab.cpachecker.core.MainStatistics;
import org.sosy_lab.cpachecker.core.algorithm.pcc.ProofGenerator;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseEmptyResult;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseResult;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseStatus;
import org.sosy_lab.cpachecker.core.phase.util.CPAPhaseExecStrategy;
import org.sosy_lab.cpachecker.core.phase.util.CPAPhases;
import org.sosy_lab.cpachecker.core.phase.util.SequentialExecStrategy;
import org.sosy_lab.cpachecker.core.phase.util.StatisticsOptions;
import org.sosy_lab.cpachecker.util.globalinfo.BasicIOManager;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * A basic analysis phase should contains two sub-phases: (1) run a single
 * algorithm, (2) collect results.
 * Some features in CPAMain originally should be migrated to this class.
 * A basic analysis phase is an "output" phase, which analyzes a project and generates statistics
 * and other analysis results.
 */
@Options(prefix = "phase.analysis")
public class BasicAnalysisPhase extends CPAPhase implements StatisticsProvider {

  @Option(secure = true, name = "type", description = "type of analysis")
  private AnalysisKind analysisKind = SINGLE;

  private final List<CPAPhase> phases;
  private final String rootDirectory;


  public BasicAnalysisPhase(
      String pIdentifier,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownManager pShutdownManager,
      ShutdownNotifier pShutdownNotifier,
      MainStatistics pStatistics)
      throws InvalidConfigurationException {
    // To prevent intervention of multiple phases, we use standalone statistics here
    super(pIdentifier, pConfig, pLogger, pShutdownManager, pShutdownNotifier, pStatistics);
    config.inject(this);
    phases = new ArrayList<>(3);

    // set-up the sub-directory and paths for file type converter
    BasicIOManager IOManager = GlobalInfo.getInstance().getIoManager();
    rootDirectory = IOManager.getBasicOutputDirectory();
    String outputDirectory = BasicIOManager.concatPath(rootDirectory, id);
    config = BasicIOManager.setupPaths(outputDirectory, config, IOManager.getSecureMode());
  }

  @Override
  protected CPAPhaseStatus prevAction() throws Exception {
    // Create three phases: CFACreatePhase, SingleAlgorithmRunPhase and AnalyzingResultPhase.
    String algRunID = id.concat(".AlgRun");
    String resultID = id.concat(".Result");
    ConfigurationBuilder configBuilder = Configuration.builder().copyFrom(config);
    Configuration newConfig = configBuilder.build();

    // Create an algorithm run phase according to the specified analysis kind
    CPAPhase algorithmRunPhase;
    switch (analysisKind) {
      case MULTI_ENTRY: {
        if (!(stats instanceof HierarchicalCPAStatistics)) {
          throw new InvalidConfigurationException("Hierarchical main statistics required for "
              + "multi-entry analysis.");
        }
        algorithmRunPhase = new MultiEntryAlgorithmRunPhase(algRunID, newConfig, logger,
            shutdownManager, shutdownNotifier, stats);
        break;
      }
      case PROGRAM_REPAIR: {
        if (!(stats instanceof HierarchicalCPAStatistics)) {
          throw new InvalidConfigurationException("Hierarchical main statistics required for "
              + "multi-entry analysis");
        }
        algorithmRunPhase = new ProgramRepairPhase(algRunID, newConfig, logger, shutdownManager,
            shutdownNotifier, stats);
        break;
      }
      default: {
        algorithmRunPhase = new SingleAlgorithmRunPhase(algRunID, newConfig, logger,
            shutdownManager, shutdownNotifier, stats);
      }
    }
    AnalyzingResultPhase resultPhase = new AnalyzingResultPhase(resultID, newConfig, logger,
        shutdownManager, shutdownNotifier, stats);

    algorithmRunPhase.addSuccessor(resultPhase);
    phases.add(algorithmRunPhase);
    phases.add(resultPhase);
    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus postAction() throws Exception {
    currResult = CPAPhaseEmptyResult.createInstance();
    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus runPhase() throws Exception {
    System.out.println("Analyzing...");

    // three phases could only be executed in sequential manner
    ProofGenerator proofGenerator = null;
    ResourceLimitChecker limits;
    StatisticsOptions options = new StatisticsOptions(config);
    try {
      limits = ResourceLimitChecker.fromConfiguration(config, logger, shutdownManager);
      limits.start();
      if (options.getDoPCC()) {
        proofGenerator = new ProofGenerator(config, logger, shutdownNotifier);
      }
    } catch (InvalidConfigurationException e) {
      logger.logUserException(Level.SEVERE, e, "Invalid configuration");
      System.exit(1);
      return CPAPhaseStatus.FAIL;
    }

    ShutdownHook shutdownHook = new ShutdownHook(shutdownManager);
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    ShutdownRequestListener forcedExitOnShutdown = ForceTerminationOnShutdown
        .createShutdownListener(logger, shutdownHook);
    shutdownNotifier.register(forcedExitOnShutdown);

    CPAPhaseExecStrategy execStg = new SequentialExecStrategy();
    CPAPhaseStatus execResult = execStg.exec(phases);

    CPAPhaseResult lastResult = phases.get(phases.size() - 1).getResult();
    if (!(lastResult instanceof CPAcheckerResult)) {
      logger.log(Level.SEVERE, "The last sub-phase of BasicAnalysisPhase must be "
          + "AnalyzingResultPhase.");
      System.exit(1);
      return CPAPhaseStatus.FAIL;
    }
    CPAcheckerResult finalResult = (CPAcheckerResult) lastResult;
    // make up CFA statistics if any
    CFACreatePhase cfaPhase = CPAPhases.extractPhaseByType(prevPhases, CFACreatePhase.class);
    if (cfaPhase != null) {
      List<Statistics> statistics = new ArrayList<>(1);
      cfaPhase.collectStatistics(statistics);
      Statistics onlyStats = statistics.get(0);
      if (onlyStats instanceof MainCPAStatistics) {
        finalResult.updateCFAStatistics((MainCPAStatistics) onlyStats);
      }
    }
    if (proofGenerator != null) {
      proofGenerator.generateProof(finalResult);
    }

    shutdownHook.disable();
    shutdownNotifier.unregister(forcedExitOnShutdown);
    ForceTerminationOnShutdown.cancelPendingTermination();
    limits.cancel();
    Thread.interrupted();

    try {
      printResultAndStatistics(finalResult, options);
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write statistics to file");
    }
    System.out.flush();
    System.err.flush();
    logger.flush();

    return execResult;
  }

  private void printResultAndStatistics(CPAcheckerResult pResult, StatisticsOptions options)
      throws IOException {
    PrintStream console = options.getPrintStatistics() ? System.out : null;
    OutputStream file = null;
    Closer closer = Closer.create();
    if (options.getExportStatistics() && options.getExportStatisticsFile() != null) {
      try {
        Path exportFile = options.getExportStatisticsFile();
        Files.createParentDirs(exportFile);
        file = closer.register(exportFile.asByteSink().openStream());
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write statistics to file");
      }
    }
    PrintStream stream = makePrintStream(mergeStreams(console, file));

    try {
      pResult.printStatistics(stream);
      stream.println();
      if (!options.getPrintStatistics()) {
        stream = makePrintStream(mergeStreams(System.out, file));
      }
      options.outputErrorReport();
      stream.println("ALARMS: " + GlobalInfo.getInstance().getBugSize());

      pResult.printResult(stream);
      if (rootDirectory != null) {
        stream.println("More details about the analysis can be found in the directory \"" +
            rootDirectory + "\".");
      }

      stream.flush();
    } catch (Throwable t) {
      throw closer.rethrow(t);
    } finally {
      closer.close();
    }
  }

  static PrintStream makePrintStream(OutputStream stream) {
    if (stream instanceof PrintStream) {
      return (PrintStream) stream;
    } else {
      return new PrintStream(stream);
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(stats);
  }

  enum AnalysisKind {
    SINGLE,
    // single entry
    MULTI_ENTRY,
    // multi-entry
    PROGRAM_REPAIR, // program repair based on multi-entry
  }

}
