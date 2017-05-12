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

import static org.sosy_lab.common.ShutdownNotifier.interruptCurrentThreadOnShutdown;
import static org.sosy_lab.cpachecker.core.CPAchecker.MainStatisticsKind.CLASSICAL;

import com.google.common.base.StandardSystemProperty;
import com.google.common.io.Resources;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.core.phase.util.CPAPhaseManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

@Options
public class CPAchecker {

  public enum InitialStatesFor {
    /**
     * Function entry node of the entry function
     */
    ENTRY,

    /**
     * Set of function entry nodes of all functions.
     */
    FUNCTION_ENTRIES,

    /**
     * All locations that are possible targets of the analysis.
     */
    TARGET,

    /**
     * Function exit node of the entry function.
     */
    EXIT,

    /**
     * All function exit nodes of all functions and all loop heads of endless loops.
     */
    FUNCTION_SINKS,

    /**
     * All function exit nodes of the entry function, and all loop heads of endless loops.
     */
    PROGRAM_SINKS
  }

  public enum MainStatisticsKind {
    CLASSICAL,
    /**
     * Hierarchical statistics distinguish per-entry / global information, which is specifically
     * designed for multi-entry analysis.
     */
    HIERARCHICAL
  }

  @Option(secure = true, name = "input.programs", description = "programs to be analyzed")
  private String programDenotations = "";

  @Option(secure = true, name = "statistics.kind", description = "the type of main statistics used"
      + " in the analysis task")
  private MainStatisticsKind mainStatisticsKind = CLASSICAL;

  private final LogManager logger;
  private final Configuration config;
  private final ShutdownManager shutdownManager;
  private final ShutdownNotifier shutdownNotifier;

  // The content of this String is read from a file that is created by the
  // ant task "init".
  // To change the version, update the property in build.xml.
  private static final String version;

  static {
    String v = "(unknown version)";
    try {
      URL url =
          CPAchecker.class.getClassLoader().getResource("org/sosy_lab/cpachecker/VERSION.txt");
      if (url != null) {
        String content = Resources.toString(url, StandardCharsets.US_ASCII).trim();
        if (content.matches("[a-zA-Z0-9 ._+:-]+")) {
          v = content;
        }
      }
    } catch (IOException e) {
      // Ignore exception, no better idea what to do here.
    }
    version = v;
  }

  public static String getVersion() {
    return getCPAcheckerVersion()
        + " (" + StandardSystemProperty.JAVA_VM_NAME.value()
        + " " + StandardSystemProperty.JAVA_VERSION.value() + ")";
  }

  public static String getCPAcheckerVersion() {
    return version;
  }

  public CPAchecker(
      Configuration pConfiguration, LogManager pLogManager,
      ShutdownManager pShutdownManager) throws InvalidConfigurationException {
    config = pConfiguration;
    logger = pLogManager;
    shutdownManager = pShutdownManager;
    shutdownNotifier = shutdownManager.getNotifier();

    config.inject(this);

    if (programDenotations.isEmpty()) {
      throw new InvalidConfigurationException("No valid input programs are specified");
    }
    GlobalInfo.getInstance().updateInputPrograms(programDenotations);
    GlobalInfo.getInstance().setUpFunctionMap(config);
  }

  public void run() throws Exception {

    // initialize necessary data structures
    MainStatistics stats;
    CPAPhaseManager phaseManager;
    final ShutdownRequestListener interruptThreadOnShutdown = interruptCurrentThreadOnShutdown();
    shutdownNotifier.register(interruptThreadOnShutdown);

    try {

      stats = createMainStatistics();

      // create phase manager and initialize it
      phaseManager = new CPAPhaseManager(config, logger, shutdownManager, shutdownNotifier, stats);
      phaseManager.initialize();
      phaseManager.execute();

    } catch (IOException e) {
      logger.logUserException(Level.SEVERE, e, "Could not read file");
    } catch (ParserException e) {
      logger.logUserException(Level.SEVERE, e, "Parsing failed");
      StringBuilder msg = new StringBuilder();
      msg.append("Please make sure that the code can be compiled by a compiler.\n");
      if (e.getLanguage() == Language.C) {
        msg.append(
            "If the code was not preprocessed, please use a C preprocessor\nor specify the -preprocess command-line argument.\n");
      }
      msg.append(
          "If the error still occurs, please send this error message\ntogether with the input file to cpachecker-users@googlegroups.com.\n");
      logger.log(Level.INFO, msg);
    } catch (InvalidConfigurationException e) {
      logger.logUserException(Level.SEVERE, e, "Invalid configuration");
    } catch (InterruptedException e) {
      // CPAchecker must exit because it was asked to
      // we return normally instead of propagating the exception
      // so we can return the partial result we have so far
      logger.logUserException(Level.WARNING, e, "Analysis interrupted");
    } catch (CPAException e) {
      logger.logUserException(Level.SEVERE, e, null);
    } finally {
      shutdownNotifier.unregister(interruptThreadOnShutdown);
    }

    logger.log(Level.FINER, "CPAchecker finished!");

  }

  private MainStatistics createMainStatistics() throws InvalidConfigurationException {
    switch (mainStatisticsKind) {
      case HIERARCHICAL:
        return new HierarchicalCPAStatistics(config, logger);
      default:
        return new MainCPAStatistics(config, logger);
    }
  }
}
