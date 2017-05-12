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
package org.sosy_lab.cpachecker.core.phase.config;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.MainStatistics;
import org.sosy_lab.cpachecker.core.phase.CPAPhase;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhaseConfigManager {

  private final String configFile;

  /**
   * Configuration of each phase should inherit the total configuration
   */
  private final Configuration parentConfig;
  private final LogManager logger;
  private final ShutdownManager shutdownManager;
  private final ShutdownNotifier shutdownNotifier;
  private final MainStatistics stats;

  private PhaseCollection collection;
  private PhaseDependency dependency;

  private final static Joiner LINE_JOINER = Joiner.on('\n').skipNulls();

  public PhaseConfigManager(
      String pConfigFile,
      Configuration pParentConfig,
      LogManager pLogger,
      ShutdownManager pShutdownManager,
      ShutdownNotifier pShutdownNotifier,
      MainStatistics pStats) {

    configFile = pConfigFile;
    // necessary data structures
    parentConfig = pParentConfig;
    logger = pLogger;
    shutdownManager = pShutdownManager;
    shutdownNotifier = pShutdownNotifier;
    stats = pStats;

    collection = null;
    dependency = null;

  }

  public void initialize() throws IOException, InvalidConfigurationException {
    String fileContent = loadFile();
    PhaseConfigParser parser = new PhaseConfigParser();
    if (!parser.parse(fileContent)) {
      throw new InvalidConfigurationException("Invalid configuration file for CPAPhase manager");
    }
    Preconditions.checkArgument(parser.isConsistent());
    Preconditions.checkArgument(parser.checkIfValidDependency());
    collection = parser.getPhases();
    dependency = parser.getDependency();
  }

  public List<CPAPhase> createPhases() throws InvalidConfigurationException {
    Preconditions.checkNotNull(collection);
    Preconditions.checkNotNull(dependency);

    Map<String, CPAPhase> createdPhases = new HashMap<>();

    // a convention: all phases should have a constructor that accepts the {@Configuration} data
    // use Java's reflection to do these magic staffs
    for (PhaseInfo info : collection) {
      String className = info.getClassName();

      try {
        // create class given class name using reflection
        Class<?> phaseClass = Class.forName(className);
        Class<?>[] paramTypes = {String.class, Configuration.class, LogManager.class,
            ShutdownManager.class, ShutdownNotifier.class, MainStatistics.class};
        Constructor<?> constructor = phaseClass.getConstructor(paramTypes);
        // build a configuration from text
        Configuration subConfig = buildConfigurationFromText(info.getConfig());
        Object[] args = {info.getIdentifier(), subConfig, logger, shutdownManager,
            shutdownNotifier, stats};
        Object phaseInstance = constructor.newInstance(args);
        if (phaseInstance instanceof CPAPhase) {
          createdPhases.put(info.getIdentifier(), (CPAPhase) phaseInstance);
        } else {
          throw new AssertionError("Trying to create a non-phase object");
        }
      } catch (ClassNotFoundException e) {
        throw new InvalidConfigurationException("Invalid class name in configuration");
      } catch (NoSuchMethodException e) {
        throw new InvalidConfigurationException("Missing required constructor");
      } catch (Exception e) {
        e.printStackTrace();
        throw new InvalidConfigurationException("Failed to instantiate the phase: " + info
            .getClassName(), e);
      }
    }

    // add dependencies on phases
    Multimap<String, String> dependMap = dependency.copyOfMap();
    if (dependMap.isEmpty()) {
      if (createdPhases.size() != 1) {
        throw new InvalidConfigurationException("Invalid dependency in configuration");
      }
    }
    for (String key : dependMap.keySet()) {
      CPAPhase keyPhase = createdPhases.get(key);
      Preconditions.checkNotNull(keyPhase);

      Collection<String> values = dependMap.get(key);
      for (String value : values) {
        // key -> value
        // this means 'key' requires 'value' to continue
        // 'value' is the ancestor of 'key'
        CPAPhase valuePhase = createdPhases.get(value);

        Preconditions.checkNotNull(valuePhase);
        valuePhase.addSuccessor(keyPhase);
      }
    }

    return FluentIterable.from(createdPhases.values()).toList();
  }

  private String loadFile() throws IOException {
    Preconditions.checkNotNull(configFile);

    List<String> buffer = new ArrayList<>();
    BufferedReader br = new BufferedReader(new FileReader(configFile));
    String lineBuffer = "";
    while ((lineBuffer = br.readLine()) != null) {
      if (lineBuffer.trim().startsWith("#")) {
        // a comment line is being read, discard it
        continue;
      }
      buffer.add(lineBuffer.trim());
    }
    br.close();
    return LINE_JOINER.join(buffer);
  }

  private Configuration buildConfigurationFromText(String configText)
      throws InvalidConfigurationException {
    ConfigurationBuilder configBuilder = Configuration.builder().copyFrom(parentConfig);
    // a platform-dependent way to get line separator
    String lineSep = System.getProperty("line.separator");
    Splitter lineSplitter = Splitter.on(lineSep).omitEmptyStrings().trimResults();
    List<String> options = lineSplitter.splitToList(configText);
    // add new options in building configuration
    for (String option : options) {
      List<String> tokens = Splitter.on("=").omitEmptyStrings().trimResults().splitToList(option);
      if (tokens.size() != 2) {
        throw new InvalidConfigurationException("Invalid option line");
      }
      configBuilder.setOption(tokens.get(0), tokens.get(1));
    }
    return configBuilder.build();
  }

}
