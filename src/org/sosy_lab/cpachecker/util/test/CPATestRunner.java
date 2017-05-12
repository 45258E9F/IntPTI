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
package org.sosy_lab.cpachecker.util.test;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.StringBuildingLogHandler;
import org.sosy_lab.cpachecker.core.CPAchecker;

import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

/**
 * Helper class for running CPA tests.
 */
public class CPATestRunner {

  public static TestResults runAndLogToSTDOUT(
      Map<String, String> pProperties,
      String pSourceCodeFilePath) throws Exception {
    return run(pProperties, pSourceCodeFilePath, true);
  }

  public static TestResults run(
      Map<String, String> pProperties,
      String pSourceCodeFilePath) throws Exception {
    return run(pProperties, pSourceCodeFilePath, false);
  }

  public static TestResults run(
      Map<String, String> pProperties,
      String pSourceCodeFilePath,
      boolean writeLogToSTDOUT) throws Exception {

    Configuration config = TestDataTools.configurationForTest()
        .setOptions(pProperties)
        .setOption("phase.analysis.programs", pSourceCodeFilePath)
        .build();

    StringBuildingLogHandler stringLogHandler = new StringBuildingLogHandler();

    Handler h;
    if (writeLogToSTDOUT) {
      h = new StreamHandler(System.out, new SimpleFormatter());
    } else {
      h = stringLogHandler;
    }

    LogManager logger = new BasicLogManager(config, h);
    ShutdownManager shutdownManager = ShutdownManager.create();
    CPAchecker cpaChecker = new CPAchecker(config, logger, shutdownManager);
    try {
      // TODO: suppress a compilation error here by returning NULL, for now
      // CPAchecker.run() should return something, instead of void
      cpaChecker.run();
      return null;
    } finally {
      logger.flush();

    }

  }
}
