/*
 * IntPTI: integer error fixing by proper-type inference
 * Copyright (c) 2017.
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

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.MainStatistics;
import org.sosy_lab.cpachecker.core.algorithm.summary.SummaryComputationAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.summary.SummaryComputer;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseStatus;
import org.sosy_lab.cpachecker.core.summary.manage.SummaryProvider;
import org.sosy_lab.cpachecker.util.Pair;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Compute summaries. The configuration follows the rule that:
 *
 * in the main configuration (summary-computation.properties) file of this phase,
 * there should be a setting like
 *
 * base = PATH_OF_PROPERTIES_FILES
 * summaries = access.properties, pointer.properties, ...
 *
 * which lists the kind of summaries that needs to be computed such that
 * 1. each file defines the configuration used for computation of this summary.
 * 2. each path is relative to the path of the main configuration file
 * 3. all summaries will be computed sequentially
 */
public class SummaryComputationPhase extends CPAPhase {

  private static final String KEY_BASE_PATH = "base";
  private static final String KEY_SUMMARIES = "summaries";
  private static final String KEY_COMPUTER_CLASS = "computer";
  private List<Pair<Configuration, SummaryComputer>> tasks;

  public SummaryComputationPhase(
      String pID, Configuration pConfig, LogManager pLogger,
      ShutdownManager pShutdownManager, ShutdownNotifier pShutdownNotifier,
      MainStatistics pStats) throws InvalidConfigurationException {
    super(pID, pConfig, pLogger, pShutdownManager, pShutdownNotifier, pStats);
    SummaryProvider.initialize(pConfig);
  }

  private List<Pair<Configuration, SummaryComputer>> buildTasks(String strSummaries) {
    Iterable<String> filenames =
        Splitter.on(",").trimResults().omitEmptyStrings().split(strSummaries);
    return FluentIterable
        .from(filenames).transform(new Function<String, Pair<Configuration, SummaryComputer>>() {
          @Override
          public Pair<Configuration, SummaryComputer> apply(String filename) {
            String absPath = null;
            try {
              if (config.hasProperty(KEY_BASE_PATH)) {
                File path = new File(config.getProperty(KEY_BASE_PATH));
                absPath = path.getAbsolutePath() + File.separator + filename;
              } else {
                absPath = filename;
              }
              Configuration config1 = Configuration.builder()
                  .loadFromFile(absPath)
                  .build();
              return Pair.of(config1, buildComputer(config1));
            } catch (Exception e) {
              throw new RuntimeException("error loading the configuration " + absPath, e);
            }
          }
        }).toList();
  }

  /**
   * Configuration for a single summary computer should contain:
   * 1. The class name of computer:
   * computer = AccessSummaryComputer
   * 2. Parameters that used to configure the SummaryComputationAlgorithm
   * 3. Parameters that used to configure the SummaryComputer
   */
  protected SummaryComputer buildComputer(Configuration config)
      throws ReflectiveOperationException {
    @SuppressWarnings("unchecked")
    Class<SummaryComputer> klass =
        (Class<SummaryComputer>) Class.forName(config.getProperty(KEY_COMPUTER_CLASS))
            .asSubclass(SummaryComputer.class);
    Constructor<SummaryComputer> constructor =
        klass.getConstructor(Configuration.class, LogManager.class, ShutdownNotifier.class);
    return constructor.newInstance(config, logger, shutdownNotifier);
  }

  @Override
  protected CPAPhaseStatus prevAction() throws Exception {
    tasks = buildTasks(config.getProperty(KEY_SUMMARIES));
    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus postAction() throws Exception {
    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus runPhase() throws Exception {
    System.out.println("Computing summary...");

    ImmutableList.Builder<Pair<String, Long>> time = ImmutableList.builder();

    for (Pair<Configuration, SummaryComputer> task : tasks) {
      Stopwatch w = Stopwatch.createStarted();
      String computerName = task.getSecond().getClass().getSimpleName();
      System.out.println("Running: " + computerName);

      SummaryComputationAlgorithm algorithm =
          new SummaryComputationAlgorithm(task.getFirst(), task.getSecond(), logger);
      algorithm.run0();

      time.add(Pair.of(computerName, w.elapsed(TimeUnit.MILLISECONDS)));
      System.out.println(String.format("%s: %.3f",
          computerName,
          w.elapsed(TimeUnit.MILLISECONDS) / 1000.0));
    }

    System.out.println("-----------------------------------------");
    for (Pair<String, Long> entry : time.build()) {
      System.out
          .print(String.format("%10.3fs: %s\n", (entry.getSecond() / 1000.0), entry.getFirst()));
    }
    System.out.println("-----------------------------------------");

    return CPAPhaseStatus.SUCCESS;
  }

}
