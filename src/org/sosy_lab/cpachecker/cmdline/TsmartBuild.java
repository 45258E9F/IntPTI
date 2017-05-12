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

import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;

import org.sosy_lab.cpachecker.util.misc.DiskUtil;

import java.util.List;

import cn.harry.builder.MakefileCaptureBuilder;
import cn.harry.captor.MakefileCapture;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class TsmartBuild {

  /**
   * Non-option arguments:
   * [make command]
   *
   * Option    Description
   * ------    -----------
   * --output  Directory of the captured files.
   * --shell   The shell command used in build.
   * --D       Define macros. eg: -D"_Noreturn=x" -D__X__
   */
  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws Exception {
    // parse options
    OptionParser tsmartOptions = new OptionParser();
    tsmartOptions.accepts("output", "Directory of the captured files.")
        .withOptionalArg().ofType(String.class);
    tsmartOptions.accepts("shell", "The shell command used in build.")
        .withOptionalArg().ofType(String.class);
    tsmartOptions.accepts("D", "define macros")
        .withOptionalArg().ofType(String.class);
    tsmartOptions.nonOptions().ofType(String.class).describedAs("make command");
    OptionSet option = tsmartOptions.parse(args);

    if (option.nonOptionArguments().size() == 0) {
      tsmartOptions.printHelpOn(System.out);
    } else {
      String makeCommand = FluentIterable.from(option.nonOptionArguments())
          .transform(Functions.toStringFunction())
          .join(Joiner.on(" "));
      // prepare directories
      String projectDirectory = "./";
      String outputDirectory = option.has("output") ? option.valueOf("output").toString() : "./";
      String shell = option.has("shell") ? option.valueOf("shell").toString() : "";

      if (DiskUtil.existsDirectory(projectDirectory)) {
        MakefileCapture captor =
            MakefileCaptureBuilder.getCaptor(projectDirectory, outputDirectory);
        if (option.has("D")) {
          captor.make(makeCommand, shell, (List<String>) option.valuesOf("D"));
        } else {
          captor.make(makeCommand, shell);
        }
      }
    }
  }
}