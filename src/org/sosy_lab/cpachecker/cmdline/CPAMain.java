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
package org.sosy_lab.cpachecker.cmdline;

import com.google.common.collect.ImmutableMap;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cmdline.CmdLineArguments.InvalidCmdlineArgumentException;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.logging.Level;

public class CPAMain {

  static final PrintStream ERROR_OUTPUT = System.err;
  static final int ERROR_EXIT_CODE = 1;

  @SuppressWarnings("resource") // We don't close LogManager
  public static void main(String[] args) throws Exception {
    Configuration cpaConfig = null;
    LogManager logManager;
    try {
      try {
        cpaConfig = createConfiguration(args);
      } catch (InvalidCmdlineArgumentException e) {
        ERROR_OUTPUT.println("Could not process command line arguments: " + e.getMessage());
        System.exit(ERROR_EXIT_CODE);
      } catch (IOException e) {
        ERROR_OUTPUT.println("Could not read config file " + e.getMessage());
        System.exit(ERROR_EXIT_CODE);
      }
      logManager = new BasicLogManager(cpaConfig);
    } catch (InvalidConfigurationException e) {
      ERROR_OUTPUT.println("Invalid configuration: " + e.getMessage());
      System.exit(ERROR_EXIT_CODE);
      return;
    }
    cpaConfig.enableLogging(logManager);

    // create everything
    final ShutdownManager shutdownManager = ShutdownManager.create();
    CPAchecker cpachecker;

    try {
      cpachecker = new CPAchecker(cpaConfig, logManager, shutdownManager);
    } catch (InvalidConfigurationException e) {
      logManager.logUserException(Level.SEVERE, e, "Invalid configuration");
      System.exit(ERROR_EXIT_CODE);
      return;
    }

    // run analysis
    cpachecker.run();

    System.out.flush();
    System.err.flush();
    logManager.flush();
  }

  // Default values for options from external libraries
  // that we want to override in CPAchecker.
  private static final ImmutableMap<String, String> EXTERN_OPTION_DEFAULTS =
      ImmutableMap.<String, String>builder()
          .put("log.level", Level.INFO.toString())
          .build();

  /**
   * Parse the command line, read the configuration file,
   * and setup the program-wide base paths.
   *
   * @return A Configuration object and the output directory.
   */
  private static Configuration createConfiguration(String[] args)
      throws InvalidConfigurationException, InvalidCmdlineArgumentException, IOException {
    // if there are some command line arguments, process them
    Map<String, String> cmdLineOptions = CmdLineArguments.processArguments(args);

    boolean secureMode = cmdLineOptions.remove(CmdLineArguments.SECURE_MODE_OPTION) != null;
    if (secureMode) {
      Configuration.enableSecureModeGlobally();
    }

    // get name of config file (may be null)
    // and remove this from the list of options (it's not a real option)
    String configFile = cmdLineOptions.remove(CmdLineArguments.CONFIGURATION_FILE_OPTION);

    // create initial configuration
    // from default values, config file, and command-line arguments
    ConfigurationBuilder configBuilder = Configuration.builder();
    configBuilder.setOptions(EXTERN_OPTION_DEFAULTS);
    if (configFile != null) {
      configBuilder.loadFromFile(configFile);
    }
    configBuilder.setOptions(cmdLineOptions);
    Configuration config = configBuilder.build();

    // add a file type converter for addressing file options
    // NOTE: if we have set output directory here, then there is no need to re-configure file
    // type converter any more
    FileTypeConverter fileTypeConverter = secureMode ?
                                          FileTypeConverter.createWithSafePathsOnly(config) :
                                          FileTypeConverter.create(config);
    GlobalInfo.getInstance().setUpBasicInfo(fileTypeConverter.getOutputDirectory(), secureMode);
    Configuration configWithFileConverter = Configuration.builder().copyFrom(config).addConverter
        (FileOption.class, fileTypeConverter).build();
    Configuration.getDefaultConverters().put(FileOption.class, fileTypeConverter);
    return configWithFileConverter;
  }

  private CPAMain() {
  } // prevent instantiation

  public static String getOutputDir() {
    return GlobalInfo.getInstance().getIoManager().getBasicOutputDirectory();
  }
}