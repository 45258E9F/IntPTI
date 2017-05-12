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
package org.sosy_lab.cpachecker.cmdline;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cn.harry.builder.MakefileCaptureBuilder;
import cn.harry.captor.MakefileCapture;
import cn.harry.captor.Task;
import cn.harry.captor.Tasks;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * Main entry for benchmark evaluation.
 */
public class TsmartMain {

  private static final String PROPERTIES = ".properties";
  private static final String TEMP = ".tmp";

  /* ******************* */
  /* configuration class */
  /* ******************* */

  private static class TsmartMainOptions {
    // TODO: add more configuration items (which could be parsed from command line parameters)

    /**
     * Options of key-value pairs that will be passed to CPAchecker
     */
    private ImmutableMap<String, String> options;
    /**
     * The root directory of configuration file.
     */
    private String rootDirectory = null;

    /**
     * The CWE number of test suite for evaluation.
     */
    private Integer weakness;

    private List<String> inputFiles = new ArrayList<>();

    private TsmartMainOptions(
        String pRootDirectory,
        Integer pWeakness,
        List<String> pInputFiles,
        ImmutableMap<String, String> pOptions) {
      this.options = pOptions == null ? ImmutableMap.<String, String>of() : pOptions;
      if (pRootDirectory != null) {
        rootDirectory = pRootDirectory;
      } else {
        rootDirectory = "./config/phase/evaluation/";
      }
      if (pWeakness == null) {
        throw new IllegalArgumentException("CWE ID required for checking");
      }
      weakness = pWeakness;
      if (pInputFiles.size() < 1) {
        throw new IllegalArgumentException("Files required for checking");
      }
      inputFiles.addAll(pInputFiles);
    }

    static TsmartMainOptions of(
        String pRootDirectory, Integer pWeakness,
        List<String> pInputFiles, ImmutableMap<String, String> pOptions) {
      return new TsmartMainOptions(pRootDirectory, pWeakness, pInputFiles, pOptions);
    }

    String getRootDirectory() {
      return rootDirectory;
    }

    Integer getWeakness() {
      return weakness;
    }

    List<String> getInputFiles() {
      return ImmutableList.copyOf(inputFiles);
    }
  }

  /* ********** */
  /* main entry */
  /* ********** */

  private static String tempConfigFile = null;

  /**
   * @param args TsmartBD [-root=dir] -cwe=IDs [-make=Makefile -directory=OutputDirectory| file1
   *             file2...] if the OutputDirectory already exits and contains tasks, we won't
   *             pre-process the project
   * @throws Exception IllegalArgumentException
   */
  public static void main(String[] args) throws Exception {
    try {
      OptionParser tsmartOptions = new OptionParser();
      tsmartOptions.accepts("root", "root directory of configuration").withOptionalArg().ofType
          (String.class);
      tsmartOptions.accepts("cwe", "cwe IDs")
          .withRequiredArg().required().withValuesSeparatedBy(',').ofType(String.class);
      tsmartOptions.accepts("make", "project folder to make")
          .withOptionalArg().ofType(String.class);
      tsmartOptions.nonOptions("files to check").ofType(File.class).describedAs("input files");
      OptionSet set = tsmartOptions.parse(args);
      String rootDir = null, cweId;
      if (set.valueOf("root") != null) {
        rootDir = set.valueOf("root").toString();
      }
      assert (!set.valuesOf("cwe").isEmpty());
      // for now only one CWE category is supported during one check
      cweId = set.valuesOf("cwe").get(0).toString();
      if (set.valueOf("make") != null) {
        String projectDirectory = set.valueOf("make").toString();
        String outputDirectory = projectDirectory;
        // captured files
        if (set.valueOf("directory") != null) {
          outputDirectory = set.valueOf("directory").toString();
        }
        String tasksFileLocation = outputDirectory + "/tasks.json";
        Tasks tasks = null;
        if (new File(tasksFileLocation).exists()) {
          tasks = MakefileCapture.getTasksFromJson(tasksFileLocation);
        } else {
          tasks = preprocess(projectDirectory);
        }
        processTasks(tasks, rootDir, cweId, null);
      } else {
        // read input files from command line arguments
        List<String> inputFiles = new ArrayList<>();
        for (Object file : set.nonOptionArguments()) {
          inputFiles.add(file.toString());
        }
        Tasks tasks = Tasks.fromTask(Task.of(inputFiles));
        processTasks(tasks, rootDir, cweId, null);
      }
    } finally {
      checkAndDeleteTempConfig();
    }
  }

  private static void checkAndDeleteTempConfig() {
    if (tempConfigFile != null) {
      File file = new File(tempConfigFile);
      if (file.exists()) {
        file.delete();
      }
    }
  }

  /**
   * Generate run argument for CPAMain.
   *
   * @return -config ***.properties.tmp
   */
  public static String[] convertArguments(
      String pRootDir,
      String pCWEId,
      List<String> pFiles,
      ImmutableMap<String, String> extOptions) {
    String vRootDir;
    Integer vCWEId;
    List<String> vFiles = new ArrayList<>(pFiles.size());
    if (pRootDir == null) {
      vRootDir = null;
    } else {
      File rootDir = new File(pRootDir);
      if (!rootDir.exists()) {
        // we use default root directory by default
        vRootDir = null;
      } else {
        vRootDir = rootDir.getAbsolutePath();
      }
    }
    vCWEId = Integer.parseInt(pCWEId);
    for (String pFile : pFiles) {
      File file = new File(pFile);
      if (!file.exists()) {
        throw new IllegalArgumentException("specified input file does not exist");
      }
      vFiles.add(file.getAbsolutePath());
    }
    TsmartMainOptions options = TsmartMainOptions.of(vRootDir, vCWEId, vFiles, extOptions);
    try {
      List<String> runArgs = generateRunArguments(options);
      return runArgs.toArray(new String[runArgs.size()]);
    } catch (IOException ex) {
      throw new IllegalArgumentException("illegal argument for Tsmart-BD, exit...");
    }
  }

  /* ******************* */
  /* argument generation */
  /* ******************* */

  private static List<String> generateRunArguments(TsmartMainOptions pOptions) throws IOException {
    String rootDir = pOptions.getRootDirectory();
    Integer cweId = pOptions.getWeakness();
    List<String> inputFiles = pOptions.getInputFiles();
    List<String> newArgs = new ArrayList<>();
    String templateName = compositeFileName(rootDir, cweId.toString() + PROPERTIES);
    File configTemplate = new File(templateName);
    if (!configTemplate.exists()) {
      throw new IllegalArgumentException("unsupported CWE: " + cweId);
    }
    createConfiguration(templateName, inputFiles, pOptions.options);
    Preconditions.checkNotNull(tempConfigFile);
    newArgs.add("-config");
    newArgs.add(tempConfigFile);
    return newArgs;
  }

  private static void createConfiguration(
      String templateName,
      List<String> inputFiles,
      ImmutableMap<String, String> options)
      throws IOException {
    tempConfigFile = templateName + TEMP;
    File tempFile = new File(tempConfigFile);
    if (!tempFile.exists()) {
      Preconditions.checkArgument(tempFile.createNewFile());
    }
    Files.copy(new File(templateName), tempFile);
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : options.entrySet()) {
      sb.append(entry.getKey() + " = " + entry.getValue() + "\n");
    }
    sb.append("input.programs = " + Joiner.on(',').join(inputFiles) + "\n");
    Files.append(sb.toString(), tempFile, Charset.defaultCharset());
  }

  /* ************** */
  /* pre-processing */
  /* ************** */

  private static boolean restoreOutputDir(String outputDirectory, String tag) {
    File output = new File(outputDirectory);
    return output.renameTo(new File(outputDirectory + tag));
  }

  /**
   * @param projectDirectory project folder
   * @return tasks list, each task includes .i files list
   */
  public static Tasks preprocess(String projectDirectory) {
    return preprocess(projectDirectory, projectDirectory, "make", "");
  }


  /**
   * @param projectDirectory project folder, outputDirectory .i files folder, makeCommand the
   *                         command to make
   * @return tasks list, each task includes .i files list
   */
  public static Tasks preprocess(
      String projectDirectory,
      String outputDirectory,
      String makeCommand,
      String shell) {
    if (new File(projectDirectory).isDirectory()) {
      MakefileCapture captor = MakefileCaptureBuilder.getCaptor(projectDirectory, outputDirectory);
      if (captor.make(makeCommand, shell)) {
        return captor.getTasks();
      }
      throw new IllegalArgumentException("Given project can not generate any tasks");
    }
    throw new IllegalArgumentException("Given argument is not a directory");
  }

  /**
   * @param tasks tasks which we captured, rootDir porject folder, cweId the Juliet test suit we
   *              test
   * @throws Exception IllegalArgumentException
   */
  public static void processTasks(
      Tasks tasks,
      String rootDir,
      String cweId,
      ImmutableMap<String, String> options) throws Exception {
    int counter = 1;
    for (int i = 0; i < tasks.size(); ++i) {
      Task task = tasks.getTask(i);
      LinkedList<String> subTask = new LinkedList<>(task.getFiles());
      try {
        CPAMain.main(convertArguments(rootDir, cweId, subTask, options));
        restoreOutputDir(CPAMain.getOutputDir(), Integer.toString(counter));
      } finally {
        // remove the temporary configuration file
        checkAndDeleteTempConfig();
      }
      counter++;
    }
  }

  /* ********* */
  /* utilities */
  /* ********* */

  private static String compositeFileName(String path, String name) {
    if (path.endsWith(File.separator)) {
      return path.concat(name);
    } else {
      return path.concat(File.separator).concat(name);
    }
  }

}