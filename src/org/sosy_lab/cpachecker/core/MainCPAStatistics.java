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
package org.sosy_lab.cpachecker.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.EXTRACT_LOCATION;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;

import org.sosy_lab.common.concurrency.Threads;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.export.DOTBuilder;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.counterexample.GenerateReportWithoutGraphs;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.IterationStatistics;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.coverage.CoverageReport;
import org.sosy_lab.cpachecker.util.resources.MemoryStatistics;
import org.sosy_lab.cpachecker.util.resources.ProcessCpuTime;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.management.JMException;

@Options
public class MainCPAStatistics implements MainStatistics {

  // Beyond this many states, we omit some statistics because they are costly.
  private static final int MAX_SIZE_FOR_REACHED_STATISTICS = 1000000;

  @Option(secure = true, name = "reachedSet.export",
      description = "print reached set to text file")
  private boolean exportReachedSet = false;

  @Option(secure = true, name = "reachedSet.file",
      description = "print reached set to text file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path reachedSetFile = Paths.get("reached.txt");

  @Option(secure = true, name = "reachedSet.dot",
      description = "print reached set to graph file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path reachedSetGraphDumpPath = Paths.get("reached.dot");

  @Option(secure = true, name = "statistics.memory",
      description = "track memory usage of JVM during runtime")
  private boolean monitorMemoryUsage = true;

  @Option(secure = true, name = "newCounterexampleReport",
      description = "insert all files except cfa/arg-graphs in html/js-template")
  private boolean generateNewCounterexampleReport = true;

  private final LogManager logger;
  private final Collection<Statistics> subStats;
  private final MemoryStatistics memStats;
  private final CoverageReport coverageReport;
  private Thread memStatsThread;

  private Collection<IterationStatistics> iterationStats;

  private final Timer programTime = new Timer();
  final Timer creationTime = new Timer();
  final Timer cpaCreationTime = new Timer();
  private final Timer analysisTime = new Timer();
  final Timer resultAnalysisTime = new Timer();

  private long programCpuTime;
  private long analysisCpuTime = 0;

  private Statistics cfaCreatorStatistics;
  private CFA cfa;

  public MainCPAStatistics(Configuration config, LogManager pLogger)
      throws InvalidConfigurationException {
    logger = pLogger;
    config.inject(this);

    subStats = new ArrayList<>();

    if (monitorMemoryUsage) {
      memStats = new MemoryStatistics(pLogger);
      memStatsThread = Threads.newThread(memStats, "CPAchecker memory statistics collector", true);
      memStatsThread.start();
    } else {
      memStats = null;
    }

    programTime.start();
    try {
      programCpuTime = ProcessCpuTime.read();
    } catch (JMException e) {
      logger.logDebugException(e, "Querying cpu time failed");
      logger.log(Level.WARNING,
          "Your Java VM does not support measuring the cpu time, some statistics will be missing.");
      programCpuTime = -1;
    }
    /*
     * Google App Engine does not allow to use classes from the package java.lang.management.
     * Therefore it throws a NoClassDefFoundError if this is attempted regardless. To prevent
     * CPAChecker from crashing in this case we catch the error and log the event.
     */ catch (NoClassDefFoundError e) {
      logger.logDebugException(e, "Querying cpu time failed");
      logger.log(Level.WARNING, "Google App Engine does not support measuring the cpu time.");
      programCpuTime = -1;
    }

    coverageReport = new CoverageReport(config, pLogger);
  }

  @Override
  public boolean transferCFAStatistics(MainStatistics pStatistics) {
    CFA mCfa = pStatistics.getCFA();
    if (mCfa != null) {
      this.cfa = mCfa;
      Statistics cfaStatistics = pStatistics.getCFAStatistics();
      if (cfaStatistics != null) {
        this.cfaCreatorStatistics = cfaStatistics;
        return true;
      }
    }
    return false;
  }

  @Override
  public CFA getCFA() {
    return cfa;
  }

  @Override
  public Statistics getCFAStatistics() {
    return cfaCreatorStatistics;
  }

  @Override
  public Collection<Statistics> getSubStatistics() {
    return subStats;
  }

  @Override
  public String getName() {
    return "CPAchecker";
  }

  @Override
  public void startAnalysisTimer() {
    analysisTime.start();
    try {
      analysisCpuTime = ProcessCpuTime.read();
    } catch (JMException e) {
      logger.logDebugException(e, "Querying cpu time failed");
      // user was already warned
      analysisCpuTime = -1;
    }
    /*
     * Google App Engine does not allow to use classes from the package java.lang.management.
     * Therefore it throws a NoClassDefFoundError if this is attempted regardless. To prevent
     * CPAChecker from crashing in this case we catch the error and log the event.
     */ catch (NoClassDefFoundError e) {
      logger.logDebugException(e, "Querying cpu time failed");
      logger.log(Level.WARNING, "Google App Engine does not support measuring the cpu time.");
      analysisCpuTime = -1;
    }
  }

  @Override
  public void stopAnalysisTimer() {
    analysisTime.stop();
    programTime.stop();

    try {
      long stopCpuTime = ProcessCpuTime.read();

      if (programCpuTime >= 0) {
        programCpuTime = stopCpuTime - programCpuTime;
      }
      if (analysisCpuTime >= 0) {
        analysisCpuTime = stopCpuTime - analysisCpuTime;
      }

    } catch (JMException e) {
      logger.logDebugException(e, "Querying cpu time failed");
      // user was already warned
    }
    /*
     * Google App Engine does not allow to use classes from the package java.lang.management.
     * Therefore it throws a NoClassDefFoundError if this is attempted regardless. To prevent
     * CPAChecker from crashing in this case we catch the error and log the event.
     */ catch (NoClassDefFoundError e) {
      logger.logDebugException(e, "Querying cpu time failed");
      logger.log(Level.WARNING, "Google App Engine does not support measuring the cpu time.");
    }
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    checkNotNull(out);
    checkNotNull(result);
    checkArgument(result == Result.NOT_YET_STARTED || reached != null);

    // call stop again in case CPAchecker was terminated abnormally
    if (analysisTime.isRunning()) {
      analysisTime.stop();
    }
    if (programTime.isRunning()) {
      programTime.stop();
    }
    if (memStats != null) {
      memStatsThread.interrupt(); // stop memory statistics collection
    }

    final Timer statisticsTime = new Timer();
    statisticsTime.start();

    if (result != Result.NOT_YET_STARTED) {
      dumpReachedSet(reached);

      printSubStatistics(out, result, reached);

      if (coverageReport != null && cfa != null) {
        coverageReport.writeCoverageReport(out, reached, cfa);
      }
    }

    out.println("CPAchecker general statistics");
    out.println("-----------------------------");

    printCfaStatistics(out);

    if (result != Result.NOT_YET_STARTED) {
      try {
        printReachedSetStatistics(reached, out);
      } catch (OutOfMemoryError e) {
        logger.logUserException(Level.WARNING, e,
            "Out of memory while generating statistics about final reached set");
      }
    }

    out.println();

    printTimeStatistics(out, result, reached, statisticsTime);

    out.println();

    printMemoryStatistics(out);

    if (generateNewCounterexampleReport && cfa != null) {
      final GenerateReportWithoutGraphs generateReportWithoutGraphs =
          new GenerateReportWithoutGraphs(logger, cfa);
      generateReportWithoutGraphs.generate();
    }
  }


  private void dumpReachedSet(ReachedSet reached) {
    dumpReachedSet(reached, reachedSetFile, false);
    dumpReachedSet(reached, reachedSetGraphDumpPath, true);
  }

  private void dumpReachedSet(ReachedSet reached, Path pOutputFile, boolean writeDotFormat) {
    assert reached != null : "ReachedSet may be null only if analysis not yet started";

    if (exportReachedSet && pOutputFile != null) {
      try (Writer w = Files.openOutputFile(pOutputFile)) {

        if (writeDotFormat) {

          // Location-map specific dump.
          dumpLocationMappedReachedSet(reached, cfa, w);
        } else {

          // Default dump.
          Joiner.on('\n').appendTo(w, reached);
        }
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write reached set to file");
      } catch (OutOfMemoryError e) {
        logger.logUserException(Level.WARNING, e,
            "Could not write reached set to file due to memory problems");
      }
    }
  }

  private void dumpLocationMappedReachedSet(
      final ReachedSet pReachedSet,
      CFA cfa,
      Appendable sb) throws IOException {
    final ListMultimap<CFANode, AbstractState> locationIndex
        = Multimaps.index(pReachedSet, EXTRACT_LOCATION);

    Function<CFANode, String> nodeLabelFormatter = new Function<CFANode, String>() {
      @Override
      public String apply(CFANode node) {
        StringBuilder buf = new StringBuilder();
        buf.append(node.getNodeNumber()).append("\n");
        for (AbstractState state : locationIndex.get(node)) {
          if (state instanceof Graphable) {
            buf.append(((Graphable) state).toDOTLabel());
          }
        }
        return buf.toString();
      }
    };
    DOTBuilder.generateDOT(sb, cfa, nodeLabelFormatter);
  }

  private void printSubStatistics(PrintStream out, Result result, ReachedSet reached) {
    assert reached != null : "ReachedSet may be null only if analysis not yet started";

    for (Statistics s : subStats) {
      String name = s.getName();
      if (!Strings.isNullOrEmpty(name)) {
        name = name + " statistics";
        out.println(name);
        out.println(Strings.repeat("-", name.length()));
      }

      try {
        s.printStatistics(out, result, reached);
      } catch (OutOfMemoryError e) {
        logger.logUserException(Level.WARNING, e,
            "Out of memory while generating statistics and writing output files");
      }

      if (!Strings.isNullOrEmpty(name)) {
        out.println();
      }
    }
  }

  private void printReachedSetStatistics(ReachedSet reached, PrintStream out) {
    assert reached != null : "ReachedSet may be null only if analysis not yet started";

    if (reached instanceof ForwardingReachedSet) {
      reached = ((ForwardingReachedSet) reached).getDelegate();
    }
    int reachedSize = reached.size();

    out.println("Size of reached set:             " + reachedSize);

    if (!reached.isEmpty()) {
      if (reachedSize < MAX_SIZE_FOR_REACHED_STATISTICS) {
        printReachedSetStatisticsDetails(reached, out);
      }

      if (reached.hasWaitingState()) {
        out.println("  Size of final wait list        " + reached.getWaitlist().size());
      }
    }
  }

  private void printReachedSetStatisticsDetails(ReachedSet reached, PrintStream out) {
    int reachedSize = reached.size();
    Set<CFANode> locations;
    CFANode mostFrequentLocation = null;
    int mostFrequentLocationCount = 0;

    if (reached instanceof LocationMappedReachedSet) {
      LocationMappedReachedSet l = (LocationMappedReachedSet) reached;
      locations = l.getLocations();

      Map.Entry<Object, Collection<AbstractState>> maxPartition = l.getMaxPartition();
      mostFrequentLocation = (CFANode) maxPartition.getKey();
      mostFrequentLocationCount = maxPartition.getValue().size();

    } else {
      HashMultiset<CFANode> allLocations = HashMultiset.create(from(reached)
          .transform(EXTRACT_LOCATION)
          .filter(notNull()));

      locations = allLocations.elementSet();

      for (Multiset.Entry<CFANode> location : allLocations.entrySet()) {
        int size = location.getCount();
        if (size > mostFrequentLocationCount) {
          mostFrequentLocationCount = size;
          mostFrequentLocation = location.getElement();

        } else if (size == mostFrequentLocationCount) {
          // use node with smallest number to have deterministic output
          mostFrequentLocation =
              Ordering.natural().min(mostFrequentLocation, location.getElement());
        }
      }
    }

    if (!locations.isEmpty()) {
      int locs = locations.size();
      out.println("  Number of reached locations:   " + locs + " (" + StatisticsUtils
          .toPercent(locs, cfa.getAllNodes().size()) + ")");
      out.println("    Avg states per location:     " + reachedSize / locs);
      out.println("    Max states per location:     " + mostFrequentLocationCount + " (at node "
          + mostFrequentLocation + ")");

      Set<String> functions = from(locations).transform(CFAUtils.GET_FUNCTION).toSet();
      out.println("  Number of reached functions:   " + functions.size() + " (" + StatisticsUtils
          .toPercent(functions.size(), cfa.getNumberOfFunctions()) + ")");
    }

    if (reached instanceof PartitionedReachedSet) {
      PartitionedReachedSet p = (PartitionedReachedSet) reached;
      int partitions = p.getNumberOfPartitions();
      out.println("  Number of partitions:          " + partitions);
      out.println("    Avg size of partitions:      " + reachedSize / partitions);
      Map.Entry<Object, Collection<AbstractState>> maxPartition = p.getMaxPartition();
      out.print("    Max size of partitions:      " + maxPartition.getValue().size());
      if (maxPartition.getValue().size() > 1) {
        out.println(" (with key " + maxPartition.getKey() + ")");
      } else {
        out.println();
      }
    }
    out.println("  Number of target states:       " + from(reached).filter(IS_TARGET_STATE).size());
  }

  private void printCfaStatistics(PrintStream out) {
    if (cfa != null) {
      int edges = 0;
      for (CFANode n : cfa.getAllNodes()) {
        edges += n.getNumEnteringEdges();
      }

      out.println("Number of program locations:     " + cfa.getAllNodes().size());
      out.println("Number of CFA edges:             " + edges);
      if (cfa.getVarClassification().isPresent()) {
        out.println("Number of relevant variables:    " + cfa.getVarClassification().get()
            .getRelevantVariables().size());
      }
      out.println("Number of functions:             " + cfa.getNumberOfFunctions());

      if (cfa.getLoopStructure().isPresent()) {
        int loops = cfa.getLoopStructure().get().getCount();
        out.println("Number of loops:                 " + loops);
      }
    }
  }

  private void printTimeStatistics(
      PrintStream out, Result result, ReachedSet reached,
      Timer statisticsTime) {
    out.println("Time for analysis setup:      " + creationTime);
    out.println("  Time for loading CPAs:      " + cpaCreationTime);
    if (cfaCreatorStatistics != null) {
      cfaCreatorStatistics.printStatistics(out, result, reached);
    }
    out.println("Time for Analysis:            " + analysisTime);
    out.println("CPU time for analysis:        " + TimeSpan.ofNanos(analysisCpuTime)
        .formatAs(TimeUnit.SECONDS));
    if (resultAnalysisTime.getNumberOfIntervals() > 0) {
      out.println("Time for analyzing result:    " + resultAnalysisTime);
    }
    out.println("Total time for CPAchecker:    " + programTime);
    out.println("Total CPU time for CPAchecker:" + TimeSpan.ofNanos(programCpuTime)
        .formatAs(TimeUnit.SECONDS));
    out.println("Time for statistics:          " + statisticsTime);
  }

  private void printMemoryStatistics(PrintStream out) {
    if (monitorMemoryUsage) {
      MemoryStatistics.printGcStatistics(out);

      if (memStats != null) {
        try {
          memStatsThread.join(); // thread should have terminated already,
          // but wait for it to ensure memory visibility
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        if (!memStatsThread.isAlive()) {
          memStats.printStatistics(out);
        }
      }
    }
  }

  @Override
  public void setCFACreator(CFACreator pCfaCreator) {
    Preconditions.checkState(cfaCreatorStatistics == null);
    cfaCreatorStatistics = pCfaCreator.getStatistics();
  }

  @Override
  public void setCFA(CFA pCfa) {
    Preconditions.checkState(cfa == null);
    cfa = pCfa;
  }

  @Override
  public void afterAlgorithmIteration(Algorithm pAlg, ReachedSet pReached) {
    if (iterationStats == null) {
      iterationStats = Lists.newArrayList();
      for (Statistics s : subStats) {
        if (s instanceof IterationStatistics) {
          iterationStats.add((IterationStatistics) s);
        }
      }
    }

    for (IterationStatistics s : iterationStats) {
      s.printIterationStatistics(System.out, pReached);
    }
  }

  @Override
  public void startCPACreationTimer() {
    cpaCreationTime.start();
  }

  @Override
  public void stopCPACreationTimer() {
    cpaCreationTime.stop();
  }
}
