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
package org.sosy_lab.cpachecker.util.globalinfo;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;

/**
 * Basic input/output information on current verification task
 */
public class BasicIOManager {

  private String programDenotation;
  private String basicOutputDirectory;
  private boolean secureMode;

  BasicIOManager(String programs, String directory, boolean mode) {
    programDenotation = programs;
    basicOutputDirectory = directory;
    secureMode = mode;
  }

  public void updateProgramNames(String programs) {
    programDenotation = programs;
  }

  public String getProgramNames() {
    return programDenotation;
  }

  public String getBasicOutputDirectory() {
    return basicOutputDirectory;
  }

  public boolean getSecureMode() {
    return secureMode;
  }

  private static final String OUTPUT_OPTION = "output.path";

  public static Configuration setupPaths(
      String newDirectory, Configuration pConfig, boolean
      pSecureMode)
      throws InvalidConfigurationException {

    ConfigurationBuilder builder = Configuration.builder();
    builder.copyFrom(pConfig);
    builder.clearOption(OUTPUT_OPTION);
    builder.setOption(OUTPUT_OPTION, newDirectory);
    Configuration newConfig = builder.build();

    FileTypeConverter fileTypeConverter = pSecureMode ?
                                          FileTypeConverter.createWithSafePathsOnly(newConfig) :
                                          FileTypeConverter.create(newConfig);

    // update file type converter since output directory may changes
    Configuration config = Configuration.builder().copyFrom(newConfig).addConverter(FileOption
        .class, fileTypeConverter).build();
    Configuration.getDefaultConverters().put(FileOption.class, fileTypeConverter);

    return config;
  }

  public static String getCurrentOutputPath(Configuration pConfig) {
    String dir = pConfig.getProperty(OUTPUT_OPTION);
    return (dir == null) ? "" : dir;
  }

  public static String concatPath(String rootDirectory, String phaseName) {
    // the default root directory: output/
    StringBuilder pathBuilder = new StringBuilder();
    pathBuilder.append(rootDirectory);
    if (!rootDirectory.endsWith("/")) {
      pathBuilder.append("/");
    }
    pathBuilder.append(phaseName).append("/");
    return pathBuilder.toString();
  }

}
