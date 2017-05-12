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
package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.bmc.BMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.counterexamplecheck.CounterexampleCheckAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.HistoryForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import javax.annotation.Nullable;

@Options(prefix = "restartAlgorithm")
public class RestartAlgorithm implements Algorithm, StatisticsProvider {

  private static final Splitter CONFIG_FILE_CONDITION_SPLITTER =
      Splitter.on("::").trimResults().limit(2);

  private static class RestartAlgorithmStatistics implements Statistics {

    private final int noOfAlgorithms;
    private final Collection<Statistics> subStats;
    private int noOfAlgorithmsUsed = 0;
    private Timer totalTime = new Timer();

    public RestartAlgorithmStatistics(int pNoOfAlgorithms) {
      noOfAlgorithms = pNoOfAlgorithms;
      subStats = new ArrayList<>();
    }

    public Collection<Statistics> getSubStatistics() {
      return subStats;
    }

    public void resetSubStatistics() {
      subStats.clear();
      totalTime = new Timer();
    }

    @Override
    public String getName() {
      return "Restart Algorithm";
    }

    private void printIntermediateStatistics(
        PrintStream out, Result result,
        ReachedSet reached) {

      String text = "Statistics for algorithm " + noOfAlgorithmsUsed + " of " + noOfAlgorithms;
      out.println(text);
      out.println(Strings.repeat("=", text.length()));

      printSubStatistics(out, result, reached);
      out.println();
    }

    @Override
    public void printStatistics(
        PrintStream out, Result result,
        ReachedSet reached) {

      out.println("Number of algorithms provided:    " + noOfAlgorithms);
      out.println("Number of algorithms used:        " + noOfAlgorithmsUsed);

      printSubStatistics(out, result, reached);
    }

    private void printSubStatistics(PrintStream out, Result result, ReachedSet reached) {
      out.println("Total time for algorithm " + noOfAlgorithmsUsed + ": " + totalTime);

      for (Statistics s : subStats) {
        String name = s.getName();
        if (!isNullOrEmpty(name)) {
          name = name + " statistics";
          out.println("");
          out.println(name);
          out.println(Strings.repeat("-", name.length()));
        }
        s.printStatistics(out, result, reached);
      }
    }

  }

  @Option(secure = true, required = true, description = "List of files with configurations to use. "
      + "A filename can be suffixed with :if-interrupted, :if-failed, and :if-terminated "
      + "which means that this configuration will only be used if the previous configuration ended with a matching condition.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<Path> configFiles;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final RestartAlgorithmStatistics stats;
  private final String filename;
  private final CFA cfa;
  private final Configuration globalConfig;

  private Algorithm currentAlgorithm;

  public RestartAlgorithm(
      Configuration config, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier, String pFilename, CFA pCfa)
      throws InvalidConfigurationException {
    config.inject(this);

    if (configFiles.isEmpty()) {
      throw new InvalidConfigurationException(
          "Need at least one configuration for restart algorithm!");
    }

    this.stats = new RestartAlgorithmStatistics(configFiles.size());
    this.logger = pLogger;
    this.shutdownNotifier = pShutdownNotifier;
    this.filename = pFilename;
    this.cfa = pCfa;
    this.globalConfig = config;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReached) throws CPAException, InterruptedException {
    checkArgument(pReached instanceof ForwardingReachedSet,
        "RestartAlgorithm needs ForwardingReachedSet");
    checkArgument(pReached.size() <= 1,
        "RestartAlgorithm does not support being called several times with the same reached set");
    checkArgument(!pReached.isEmpty(), "RestartAlgorithm needs non-empty reached set");

    ForwardingReachedSet reached = (ForwardingReachedSet) pReached;

    Iterable<CFANode> initialNodes = AbstractStates.extractLocations(pReached.getFirstState());
    assert initialNodes != null : "Location information needed";
    CFANode mainFunction = Iterables.getOnlyElement(initialNodes);

    PeekingIterator<Path> configFilesIterator = Iterators.peekingIterator(configFiles.iterator());

    AlgorithmStatus status = AlgorithmStatus.UNSOUND_AND_PRECISE;
    while (configFilesIterator.hasNext()) {
      stats.totalTime.start();
      @Nullable
      ConfigurableProgramAnalysis currentCpa = null;
      ReachedSet currentReached;
      ShutdownManager singleShutdownManager = ShutdownManager.createWithParent(shutdownNotifier);

      boolean lastAnalysisInterrupted = false;
      boolean lastAnalysisFailed = false;
      boolean lastAnalysisTerminated = false;
      boolean recursionFound = false;
      boolean concurrencyFound = false;

      try {
        Path singleConfigFileName = configFilesIterator.next();
        // extract first part out of file name
        singleConfigFileName = Paths.get(
            CONFIG_FILE_CONDITION_SPLITTER.split(singleConfigFileName.toString()).iterator()
                .next());

        try {
          Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> currentAlg =
              createNextAlgorithm(singleConfigFileName, mainFunction, singleShutdownManager);
          currentAlgorithm = currentAlg.getFirst();
          currentCpa = currentAlg.getSecond();
          currentReached = currentAlg.getThird();
        } catch (InvalidConfigurationException e) {
          logger.logUserException(Level.WARNING, e,
              "Skipping one analysis because the configuration file " + singleConfigFileName
                  .toString() + " is invalid");
          continue;
        } catch (IOException e) {
          logger.logUserException(Level.WARNING, e,
              "Skipping one analysis because the configuration file " + singleConfigFileName
                  .toString() + " could not be read");
          continue;
        }

        if (reached instanceof HistoryForwardingReachedSet) {
          ((HistoryForwardingReachedSet) reached).saveCPA(currentCpa);
        }
        reached.setDelegate(currentReached);

        if (currentAlgorithm instanceof StatisticsProvider) {
          ((StatisticsProvider) currentAlgorithm).collectStatistics(stats.getSubStatistics());
        }
        shutdownNotifier.shutdownIfNecessary();

        stats.noOfAlgorithmsUsed++;

        // run algorithm
        try {
          status = currentAlgorithm.run(currentReached);

          if (from(currentReached).anyMatch(IS_TARGET_STATE) && status.isPrecise()) {

            // If the algorithm is not _precise_, verdict "false" actually means "unknown".
            return status;
          }

          if (!status.isSound()) {
            // if the analysis is not sound and we can proceed with
            // another algorithm, continue with the next algorithm
            logger.log(Level.INFO, "Analysis result was unsound.");

          } else if (currentReached.hasWaitingState()) {
            // if there are still states in the waitlist, the result is unknown
            // continue with the next algorithm
            logger
                .log(Level.INFO, "Analysis not completed: There are still states to be processed.");

          } else {
            // sound analysis and completely finished, terminate
            return status;
          }
          lastAnalysisTerminated = true;

        } catch (CPAException e) {
          lastAnalysisFailed = true;
          if (e.getMessage().contains("Counterexample could not be analyzed")) {
            status = status.withPrecise(false);
          }
          if (configFilesIterator.hasNext()) {
            logger.logUserException(Level.WARNING, e, "Analysis not completed");
            if (e.getMessage().contains("recursion")) {
              recursionFound = true;
            }
            if (e.getMessage().contains("pthread_create")) {
              concurrencyFound = true;
            }
          } else {
            throw e;
          }
        } catch (InterruptedException e) {
          lastAnalysisInterrupted = true;
          shutdownNotifier.shutdownIfNecessary(); // check if we should also stop
          logger.logUserException(Level.WARNING, e,
              "Analysis " + stats.noOfAlgorithmsUsed + " stopped");
        }
      } finally {
        singleShutdownManager
            .requestShutdown("Analysis terminated"); // shutdown any remaining components
        stats.totalTime.stop();
      }

      shutdownNotifier.shutdownIfNecessary();

      if (configFilesIterator.hasNext()) {
        // Check if the next config file has a condition,
        // and if it has a condition, check if it matches.
        boolean foundConfig;
        do {
          foundConfig = true;
          String nextConfigFile = configFilesIterator.peek().toString();
          List<String> parts = CONFIG_FILE_CONDITION_SPLITTER.splitToList(nextConfigFile);
          if (parts.size() == 2) {
            String condition = parts.get(1);
            switch (condition) {
              case "if-interrupted":
                foundConfig = lastAnalysisInterrupted;
                break;
              case "if-failed":
                foundConfig = lastAnalysisFailed;
                break;
              case "if-terminated":
                foundConfig = lastAnalysisTerminated;
                break;
              case "if-recursive":
                foundConfig = recursionFound;
                break;
              case "if-concurrent":
                foundConfig = concurrencyFound;
                break;
              default:
                logger.logf(Level.WARNING, "Ignoring invalid restart condition '%s'.", condition);
                foundConfig = true;
            }
            if (!foundConfig) {
              logger.logf(Level.INFO,
                  "Ignoring restart configuration '%s' because condition %s did not match.",
                  parts.get(0), condition);
              configFilesIterator.next();
              stats.noOfAlgorithmsUsed++;
            }
          }
        } while (!foundConfig && configFilesIterator.hasNext());
      }

      if (configFilesIterator.hasNext()) {
        stats.printIntermediateStatistics(System.out, Result.UNKNOWN, currentReached);
        stats.resetSubStatistics();

        if (currentCpa != null) {
          CPAs.closeCpaIfPossible(currentCpa, logger);
        }
        CPAs.closeIfPossible(currentAlgorithm, logger);

        logger.log(Level.INFO, "RestartAlgorithm switches to the next configuration...");
      }
    }

    // no further configuration available, and analysis has not finished
    logger.log(Level.INFO, "No further configuration available.");
    return status;
  }

  @Options
  private static class RestartAlgorithmOptions {

    @Option(secure = true, name = "analysis.collectAssumptions",
        description = "use assumption collecting algorithm")
    boolean collectAssumptions = false;

    @Option(secure = true, name = "analysis.algorithm.CEGAR",
        description = "use CEGAR algorithm for lazy counter-example guided analysis"
            + "\nYou need to specify a refiner with the cegar.refiner option."
            + "\nCurrently all refiner require the use of the ARGCPA.")
    boolean useCEGAR = false;

    @Option(secure = true, name = "analysis.checkCounterexamples",
        description = "use a second model checking run (e.g., with CBMC or a different CPAchecker configuration) to double-check counter-examples")
    boolean checkCounterexamples = false;

    @Option(secure = true, name = "analysis.algorithm.BMC",
        description = "use a BMC like algorithm that checks for satisfiability "
            + "after the analysis has finished, works only with PredicateCPA")
    boolean useBMC = false;

    @Option(secure = true, name = "analysis.algorithm.CBMC",
        description = "use CBMC as an external tool from CPAchecker")
    boolean runCBMCasExternalTool = false;

    @Option(secure = true, name = "analysis.unknownIfUnrestrictedProgram",
        description = "stop the analysis with the result unknown if the program does not satisfies certain restrictions.")
    private boolean unknownIfUnrestrictedProgram = false;


  }

  private Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> createNextAlgorithm(
      Path singleConfigFileName,
      CFANode mainFunction,
      ShutdownManager singleShutdownManager)
      throws InvalidConfigurationException, CPAException, IOException {

    ReachedSet reached;
    ConfigurableProgramAnalysis cpa;
    Algorithm algorithm;

    ConfigurationBuilder singleConfigBuilder = Configuration.builder();
    singleConfigBuilder.copyFrom(globalConfig);
    singleConfigBuilder.clearOption("restartAlgorithm.configFiles");
    singleConfigBuilder.clearOption("analysis.restartAfterUnknown");
    singleConfigBuilder.loadFromFile(singleConfigFileName);
    if (globalConfig.hasProperty("specification")) {
      singleConfigBuilder.copyOptionFrom(globalConfig, "specification");
    }
    Configuration singleConfig = singleConfigBuilder.build();
    LogManager singleLogger = logger.withComponentName("Analysis" + (stats.noOfAlgorithmsUsed + 1));

    RestartAlgorithmOptions singleOptions = new RestartAlgorithmOptions();
    singleConfig.inject(singleOptions);

    ResourceLimitChecker singleLimits =
        ResourceLimitChecker.fromConfiguration(singleConfig, singleLogger, singleShutdownManager);
    singleLimits.start();

    if (singleOptions.runCBMCasExternalTool) {
      algorithm = new ExternalCBMCAlgorithm(filename, singleConfig, singleLogger);
      cpa = null;
      reached = new ReachedSetFactory(singleConfig).create();
    } else {
      ReachedSetFactory singleReachedSetFactory = new ReachedSetFactory(singleConfig);
      cpa = createCPA(singleReachedSetFactory, singleConfig, singleLogger,
          singleShutdownManager.getNotifier(), stats);
      algorithm = createAlgorithm(cpa, singleConfig, singleLogger, singleShutdownManager,
          singleReachedSetFactory, singleOptions);
      reached = createInitialReachedSetForRestart(cpa, mainFunction, singleReachedSetFactory,
          singleLogger);
    }

    return Triple.of(algorithm, cpa, reached);
  }

  private ReachedSet createInitialReachedSetForRestart(
      ConfigurableProgramAnalysis cpa,
      CFANode mainFunction,
      ReachedSetFactory pReachedSetFactory,
      LogManager singleLogger) {
    singleLogger.log(Level.FINE, "Creating initial reached set");

    AbstractState initialState =
        cpa.getInitialState(mainFunction, StateSpacePartition.getDefaultPartition());
    Precision initialPrecision =
        cpa.getInitialPrecision(mainFunction, StateSpacePartition.getDefaultPartition());

    ReachedSet reached = pReachedSetFactory.create();
    reached.add(initialState, initialPrecision);
    return reached;
  }

  private ConfigurableProgramAnalysis createCPA(
      ReachedSetFactory pReachedSetFactory,
      Configuration pConfig, LogManager singleLogger, ShutdownNotifier singleShutdownNotifier,
      RestartAlgorithmStatistics stats) throws InvalidConfigurationException, CPAException {
    singleLogger.log(Level.FINE, "Creating CPAs");

    CPABuilder builder =
        new CPABuilder(pConfig, singleLogger, singleShutdownNotifier, pReachedSetFactory);
    ConfigurableProgramAnalysis cpa = builder.buildCPAWithSpecAutomatas(cfa);

    if (cpa instanceof StatisticsProvider) {
      ((StatisticsProvider) cpa).collectStatistics(stats.getSubStatistics());
    }
    return cpa;
  }

  private Algorithm createAlgorithm(
      final ConfigurableProgramAnalysis cpa, Configuration pConfig,
      final LogManager singleLogger,
      final ShutdownManager singleShutdownManager,
      ReachedSetFactory singleReachedSetFactory,
      RestartAlgorithmOptions pOptions)
      throws InvalidConfigurationException, CPAException {
    ShutdownNotifier singleShutdownNotifier = singleShutdownManager.getNotifier();
    singleLogger.log(Level.FINE, "Creating algorithms");

    Algorithm algorithm = CPAAlgorithm.create(cpa, singleLogger, pConfig, singleShutdownNotifier);

    if (pOptions.useCEGAR) {
      algorithm = new CEGARAlgorithm(algorithm, cpa, pConfig, singleLogger);
    }

    if (pOptions.useBMC) {
      algorithm = new BMCAlgorithm(algorithm, cpa, pConfig, singleLogger, singleReachedSetFactory,
          singleShutdownManager, cfa);
    }

    if (pOptions.checkCounterexamples) {
      algorithm = new CounterexampleCheckAlgorithm(algorithm, cpa, pConfig, singleLogger,
          singleShutdownNotifier, cfa, filename);
    }

    if (pOptions.collectAssumptions) {
      algorithm = new AssumptionCollectorAlgorithm(algorithm, cpa, cfa, shutdownNotifier, pConfig,
          singleLogger);
    }

    if (pOptions.unknownIfUnrestrictedProgram) {
      algorithm = new RestrictedProgramDomainAlgorithm(algorithm, cfa);
    }

    return algorithm;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (currentAlgorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) currentAlgorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
  }
}
