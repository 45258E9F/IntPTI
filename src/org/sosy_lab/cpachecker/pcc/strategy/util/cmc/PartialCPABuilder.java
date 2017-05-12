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
package org.sosy_lab.cpachecker.pcc.strategy.util.cmc;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

@Options(prefix = "pcc.cmc")
public class PartialCPABuilder {

  @Option(secure = true, description = "List of files with configurations to use. ")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<Path> configFiles;

  private final LogManager logger;
  private final Configuration globalConfig;
  private final CFA cfa;
  private final ShutdownNotifier shutdown;

  public PartialCPABuilder(
      final Configuration config, final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final CFA pCfa)
      throws InvalidConfigurationException {
    config.inject(this);
    globalConfig = config;
    logger = pLogger;
    cfa = pCfa;
    shutdown = pShutdownNotifier;
  }

  public ConfigurableProgramAnalysis buildPartialCPA(
      int iterationNumber,
      ReachedSetFactory pFactory)
      throws InvalidConfigurationException, CPAException {
    // create configuration for current partial ARG checking
    logger.log(Level.FINEST, "Build CPA configuration");
    ConfigurationBuilder singleConfigBuilder = Configuration.builder();
    singleConfigBuilder.copyFrom(globalConfig);
    try {
      if (configFiles == null) {
        throw new InvalidConfigurationException(
            "Require that option pcc.arg.cmc.configFiles is set for proof checking");
      }
      singleConfigBuilder.loadFromFile(configFiles.get(iterationNumber));
    } catch (IOException e) {
      throw new InvalidConfigurationException(
          "Cannot read configuration for current partial ARG checking.");
    }
    if (globalConfig.hasProperty("specification")) {
      singleConfigBuilder.copyOptionFrom(globalConfig, "specification");
    }
    Configuration singleConfig = singleConfigBuilder.build();

    // create CPA to check current partial ARG
    logger.log(Level.FINEST, "Create CPA instance");

    return new CPABuilder(singleConfig, logger, shutdown, pFactory).buildCPAWithSpecAutomatas(cfa);
  }


}
