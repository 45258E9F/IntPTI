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

  private String toolDirectory;

  BasicIOManager(String pRootDir) {
    toolDirectory = pRootDir;
    // other fields are initialized minimally
    programDenotation = "";
    basicOutputDirectory = "";
    secureMode = true;
  }

  void updateProgramNames(String programs) {
    programDenotation = programs;
  }

  void updateBasicInfo(String pOutputDir, boolean pSecure) {
    basicOutputDirectory = pOutputDir;
    secureMode = pSecure;
  }

  public String getProgramNames() {
    return programDenotation;
  }

  public String getBasicOutputDirectory() {
    return basicOutputDirectory;
  }

  public String getRootDirectory() {
    return toolDirectory;
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
