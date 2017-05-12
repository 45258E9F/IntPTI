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
package org.sosy_lab.cpachecker.util.coverage;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.coverage.FileCoverageInformation.FunctionInfo;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Level;

/**
 * Generate coverage information in Gcov format
 * (http://gcc.gnu.org/onlinedocs/gcc/Gcov.html).
 */
@Options
class CoverageReportGcov implements CoverageWriter {

  @Option(secure = true,
      name = "coverage.export",
      description = "print coverage info to file")
  private boolean exportCoverage = true;

  @Option(secure = true,
      name = "coverage.file",
      description = "print coverage info to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path outputCoverageFile = Paths.get("coverage.info");

  //String constants from gcov format
  private final static String TEXTNAME = "TN:";
  private final static String SOURCEFILE = "SF:";
  private final static String FUNCTION = "FN:";
  private final static String FUNCTIONDATA = "FNDA:";
  private final static String LINEDATA = "DA:";

  private final LogManager logger;

  public CoverageReportGcov(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {

    pConfig.inject(this);

    this.logger = pLogger;
  }

  @Override
  public void write(Map<String, FileCoverageInformation> pCoverage, PrintStream pStdOut) {

    if (!exportCoverage || (outputCoverageFile == null)) {
      return;
    }

    try (Writer w = Files.openOutputFile(outputCoverageFile)) {

      for (Map.Entry<String, FileCoverageInformation> entry : pCoverage.entrySet()) {
        String sourcefile = entry.getKey();
        FileCoverageInformation fileInfos = entry.getValue();

        //Convert ./test.c -> /full/path/test.c
        w.append(TEXTNAME + "\n");
        w.append(SOURCEFILE + Paths.get(sourcefile).getAbsolutePath() + "\n");

        for (FunctionInfo info : fileInfos.allFunctions) {
          w.append(FUNCTION + info.firstLine + "," + info.name + "\n");
          //Information about function end isn't used by lcov, but it is useful for some postprocessing
          //But lcov ignores all unknown lines, so, this additional information can't affect on its work
          w.append("#" + FUNCTION + info.lastLine + "\n");
        }

        for (String name : fileInfos.visitedFunctions.keySet()) {
          w.append(FUNCTIONDATA + fileInfos.visitedFunctions.get(name) + "," + name + "\n");
        }

        /* Now save information about lines
         */
        for (Integer line : fileInfos.allLines) {
          w.append(LINEDATA + line + "," + fileInfos.getVisitedLine(line) + "\n");
        }
        w.append("end_of_record\n");
      }

    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write coverage information to file");
    }

  }

}
