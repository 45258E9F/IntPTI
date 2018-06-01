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

import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import cn.harry.captor.MakefileCapture;
import cn.harry.captor.Task;
import cn.harry.captor.Tasks;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class TsmartAnalyze {
  // FIXME: we fixed the top configuration here
  private static final String BASE_TOP_CONFIG_FILE = "config/fix_top/top.properties";
  private static final String TEMP_TOP_CONFIG_FILE = "config/fix_top/top-runtime.properties";

  /**
   * Non-option arguments:
   * [manually specified input files.]
   * Option            Description
   * ------            -----------
   * --build           project folder to make
   * --captured        location of the captured build
   * --cwe             cwe IDs
   * --list            list tasks
   * --manual          manually specify the files
   * --output [File]   analyse result output location
   * --root            root directory of configuration files
   * --task [Integer]  task number
   * --taskName        task name
   */
  public static void main(String[] args) throws Exception {
    OptionParser tsmartOptions = new OptionParser();
    try {
      // one of 'build', 'captured' or 'file' should be given
      // build:     build a project before analysis
      // captured:  analyze a captured build process
      // manual:    manually specify the set of files
      tsmartOptions.accepts("build", "project folder to make")
          .withOptionalArg().ofType(String.class);
      tsmartOptions.accepts("captured", "location of the captured build")
          .withOptionalArg().ofType(String.class);
      tsmartOptions.accepts("assembled", "location of the assembled sources")
          .withOptionalArg().ofType(String.class);
      tsmartOptions.accepts("manual", "manually specify the files");

      tsmartOptions.accepts("cwe", "cwe IDs")
          .withRequiredArg().withValuesSeparatedBy(',').ofType(String.class);
      tsmartOptions.accepts("root", "root directory of configuration files")
          .withOptionalArg().ofType(String.class);
      tsmartOptions.accepts("output", "analyse result output location").withOptionalArg()
          .ofType(File.class);
      tsmartOptions.accepts("list", "list tasks")
          .withOptionalArg();
      tsmartOptions.accepts("task", "task number")
          .withRequiredArg().ofType(Integer.class);
      tsmartOptions.accepts("taskName", "task name")
          .withRequiredArg().ofType(String.class);
      tsmartOptions.nonOptions().describedAs("manully specified input files.");
      OptionSet option = tsmartOptions.parse(args);

//     TODO: add bug visualizer
//     String outputLocation = null;
//     if(set.has("output")) {
//       outputLocation = set.valueOf("output").toString();
//     }

      if (option.has("build")) {
        buildAndAnalyze(option);
      } else if (option.has("captured")) {
        analyzeCaptured(option);
      } else if (option.has("assembled")) {
        analyzeAssembled(option);
      } else if (option.has("manual")) {
        analyzeSpecified(option);
      } else {
        throw new IllegalArgumentException("One of build/captured/manual should be specified.");
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Error: " + e.getMessage());
      tsmartOptions.printHelpOn(System.out);
    }
  }

  private static void buildAndAnalyze(OptionSet option) throws Exception {
    String projectDirectory = option.valueOf("build").toString();

    StringBuilder makeCommand = new StringBuilder();
    for (Object command : option.nonOptionArguments()) {
      makeCommand.append(command.toString());
    }

    Tasks tasks = null;
    if (makeCommand.toString().startsWith("make")) {
      tasks = TsmartMain.preprocess(projectDirectory, projectDirectory, makeCommand.toString(), "");
    }

    analyze(option, tasks);
  }

  private static void analyzeCaptured(OptionSet option) throws Exception {
    Tasks tasks;

    String capturedLocation = option.valueOf("captured").toString();
    tasks = MakefileCapture.getTasksFromJson(capturedLocation + File.separator + "tasks.json");

    if (option.has("list")) {
      // list only
      printTasks(tasks);
    } else {
      // analyze

      // target tasks
      if (option.has("task")) {
        Integer taskId = (Integer) option.valueOf("task");
        if (taskId < 1 || taskId > tasks.size()) {
          throw new IllegalArgumentException(
              "Invalid task id " + taskId + ", should with [1, " + tasks.size() + "]");
        }
        tasks = Tasks.fromTask(tasks.getTask(taskId - 1));
      }

      if (option.has("taskName")) {
        String taskName = (String) option.valueOf("taskName");
        Task task = tasks.getTask(taskName);
        if (task == null) {
          throw new IllegalArgumentException(
              "Invalid task name " + taskName + ", should within [" + tasks.toString() + "]");
        }
        tasks = Tasks.fromTask(task);
      }

      analyze(option, tasks);
    }

  }

  private static void printTasks(Tasks tasks) {
    System.out.println(tasks);
  }

  private static void analyzeAssembled(OptionSet option) throws Exception {
    // in this mode, each source file under the destination location is self-contained
    String assembleLocation = option.valueOf("assembled").toString();
    final List<String> inputFiles = new ArrayList<>();
    Path asmLoc = Paths.get(assembleLocation);
    java.nio.file.Files.walkFileTree(asmLoc, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(
          Path pPath, BasicFileAttributes pBasicFileAttributes) throws IOException {
        String absPath = pPath.toFile().getAbsolutePath();
        if (absPath.endsWith(".c") || absPath.endsWith(".i")) {
          inputFiles.add(absPath);
        }
        return FileVisitResult.CONTINUE;
      }
    });
    analyze(option, Tasks.fromTask(Task.of(inputFiles)));

  }

  private static void analyzeSpecified(OptionSet option) throws Exception {
    List<String> inputFiles = new ArrayList<>();
    for (Object file : option.nonOptionArguments()) {
      inputFiles.add(file.toString());
    }
    analyze(option, Tasks.fromTask(Task.of(inputFiles)));
  }

  private static void analyze(OptionSet option, Tasks tasks) throws Exception {
    String rootDir;
    if (option.has("root")) {
      rootDir = option.valueOf("root").toString();
    } else {
      rootDir = System.getProperty("user.dir");
    }
    if (rootDir == null) {
      throw new IllegalArgumentException("Root directory required for running the tool");
    }
    // setup tool directory
    GlobalInfo.getInstance().setUpToolDirectory(rootDir);

    List<String> cweIds = null;
    if (option.has("cwe")) {
      cweIds = Lists.transform(option.valuesOf("cwe"), Functions.toStringFunction());
    }

    processTasks(tasks, rootDir, cweIds);
  }

  private static void processTasks(
      Tasks pTasks,
      String pRootDir,
      List<String> pCweIds) {
    for (int i = 0; i < pTasks.size(); ++i) {
      Task task = pTasks.getTask(i);
      // TODO: debugging
      System.out.println("Proceeding task " + (i + 1) + " : " + task.getTaskName());
      try {
        String[] arguments = prepare(task, pRootDir, pCweIds);
        CPAMain.main(arguments);
        restoreOutputDir(CPAMain.getOutputDir(), "" + (i + 1));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private static void restoreOutputDir(String outputDirectory, String name) {
    File output = new File(outputDirectory);
    output.renameTo(new File(outputDirectory + "-" + name));
  }

  /**
   * Create configuration files and generate arguments to CPAMain
   */
  private static String[] prepare(Task pTask, String pRootDir, List<String> pCweIds)
      throws IOException {
    // create configuration file
    String actualBaseTopConfig = BASE_TOP_CONFIG_FILE;
    String actualTempTopConfig = TEMP_TOP_CONFIG_FILE;
    if (pRootDir != null) {
      actualBaseTopConfig = Paths.get(pRootDir, BASE_TOP_CONFIG_FILE).toString();
      actualTempTopConfig = Paths.get(pRootDir, TEMP_TOP_CONFIG_FILE).toString();
    }
    List<String> contents =
        Files.readLines(new File(actualBaseTopConfig), Charset.defaultCharset());
    contents.add("input.programs = " + Joiner.on(", ").join(pTask.getFiles()));
    Files.write(Joiner.on("\n").join(contents),
        new File(actualTempTopConfig), Charset.defaultCharset());
    // TODO: currently ignore the CWE id, check all defects
    return new String[]{
        "-config", actualTempTopConfig
    };
  }

}