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
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.CFAUtils;
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

/**
 * A hierarchical statistics collector for multi-entry analysis.
 */
@Options
public class HierarchicalCPAStatistics implements MainStatistics {

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

  private final LogManager logger;

  // per-entry statistics
  private final Collection<Statistics> subStats;

  // global statistics
  private final MemoryStatistics memStats;
  private Thread memStatsThread;
  private final Timer programTime = new Timer();
  private final Timer cpaCreationTime = new Timer();
  private final Timer analysisTime = new Timer();

  private long programCpuTime;
  private long analysisCpuTime = 0;

  private Statistics cfaCreatorStatistics;
  private CFA cfa;

  public HierarchicalCPAStatistics(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    logger = pLogger;
    pConfig.inject(this);

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
    } catch (JMException ex) {
      logger.logDebugException(ex, "Querying cpu time failed");
      logger.log(Level.WARNING,
          "Your Java VM does not support measuring the cpu time, some statistics will be missing.");
      programCpuTime = -1;
    } catch (NoClassDefFoundError ex) {
      logger.logDebugException(ex, "Querying cpu time failed");
      logger.log(Level.WARNING, "Google App Engine does not support measuring the cpu time.");
      programCpuTime = -1;
    }
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
    } catch (NoClassDefFoundError e) {
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
    } catch (NoClassDefFoundError e) {
      logger.logDebugException(e, "Querying cpu time failed");
      logger.log(Level.WARNING, "Google App Engine does not support measuring the cpu time.");
    }
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
  public void setCFACreator(CFACreator pCFACreator) {
    Preconditions.checkState(cfaCreatorStatistics == null);
    cfaCreatorStatistics = pCFACreator.getStatistics();
  }

  @Override
  public void setCFA(CFA pCFA) {
    Preconditions.checkState(cfa == null);
    cfa = pCFA;
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
  public void afterAlgorithmIteration(
      Algorithm alg, ReachedSet reached) {
    // for now, we do nothing for iteration statistics
  }

  /**
   * Print the entry-specific statistics.
   * For each entry, we print statistics on: (1) reached set, (2) analysis from the current entry.
   *
   * @param pConfig configuration containing new OUTPUT_FILE base location.
   */
  public void printEntryStatistics(
      PrintStream out, CPAcheckerResult pResult, Configuration
      pConfig) {
    Result result = pResult.getResult();
    ReachedSet reached = pResult.getReached();

    if (result != Result.NOT_YET_STARTED) {
      dumpReachedSet(reached);
      printSubStatistics(out, result, reached, pConfig);
      try {
        printReachedSetStatistics(reached, out);
      } catch (OutOfMemoryError ex) {
        logger.logUserException(Level.WARNING, ex,
            "Out of memory while generating statistics about final reached set");
      }
    }
  }

  /* ********************** */
  /* reached set statistics */
  /* ********************** */

  private void dumpReachedSet(ReachedSet pReached) {
    dumpReachedSet(pReached, reachedSetFile, false);
    dumpReachedSet(pReached, reachedSetGraphDumpPath, true);
  }

  private void dumpReachedSet(ReachedSet reached, Path outputFile, boolean writeDotFormat) {
    if (exportReachedSet) {
      try (Writer w = Files.openOutputFile(outputFile)) {
        if (writeDotFormat) {
          dumpLocationMappedReachedSet(reached, cfa, w);
        } else {
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

  private void dumpLocationMappedReachedSet(final ReachedSet pReached, CFA pCfa, Appendable sb)
      throws IOException {
    final ListMultimap<CFANode, AbstractState> locationIndex = Multimaps.index(pReached,
        EXTRACT_LOCATION);
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
    DOTBuilder.generateDOT(sb, pCfa, nodeLabelFormatter);
  }

  private void printReachedSetStatistics(ReachedSet pReached, PrintStream out) {
    ReachedSet reached = pReached;
    if (pReached instanceof ForwardingReachedSet) {
      reached = ((ForwardingReachedSet) pReached).getDelegate();
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

  /* ************** */
  /* sub-statistics */
  /* ************** */

  private void printSubStatistics(
      PrintStream out, Result result, ReachedSet reached,
      Configuration pConfig) {

    Preconditions.checkArgument(result == Result.NOT_YET_STARTED || reached != null);

    for (Statistics s : subStats) {
      String name = s.getName();
      if (!Strings.isNullOrEmpty(name)) {
        name = name + " statistics";
        out.println(name);
        out.println(Strings.repeat("-", name.length()));
      }

      try {
        pConfig.inject(s);
      } catch (InvalidConfigurationException e) {
        logger.logUserException(Level.WARNING, e,
            "Cannot inject the new configuration into statistics object");
      } catch (Exception e) {
        // this class cannot be injected because it does not have @Option annotation
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

  /**
   * Print the global statistics.
   *
   * @param out     the PrintStream to use for printing the statistics
   * @param result  the result of the analysis
   * @param reached the final reached set
   */
  @Override
  public void printStatistics(
      PrintStream out, Result result, ReachedSet reached) {
    Preconditions.checkNotNull(out);
    Preconditions.checkNotNull(result);
    // reached set can be NULL

    if (analysisTime.isRunning()) {
      analysisTime.stop();
    }
    if (programTime.isRunning()) {
      programTime.stop();
    }
    if (memStats != null) {
      memStatsThread.interrupt(); // stop memory statistics collection
    }

    // print CFA statistics
    printCfaStatistics(out);

    // print global time statistics
    printTimeStatistics(out);

    // print global memory statistics
    printMemoryStatistics(out);

  }

  /* ***************** */
  /* global statistics */
  /* ***************** */

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

  private void printTimeStatistics(PrintStream out) {
    out.println("  Time for loading CPAs:      " + cpaCreationTime);
    out.println("Time for Analysis:            " + analysisTime);
    out.println("CPU time for analysis:        " + TimeSpan.ofNanos(analysisCpuTime).formatAs(
        TimeUnit.SECONDS));
    out.println("Total time for CPAchecker:    " + programTime);
    out.println("Total CPU time for CPAchecker:" + TimeSpan.ofNanos(programCpuTime)
        .formatAs(TimeUnit.SECONDS));
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
  public String getName() {
    return "CPAchecker MultiEntry Mode";
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
