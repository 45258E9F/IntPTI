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
package org.sosy_lab.cpachecker.core.phase.util;

import static org.sosy_lab.cpachecker.core.phase.util.StatisticsOptions.ErrorOutputMode.LOG;
import static org.sosy_lab.cpachecker.core.phase.util.StatisticsOptions.ErrorOutputMode.XML;

import com.google.common.collect.Sets;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.weakness.BugSummary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Set;

import cn.edu.thu.tsmart.tool.bd.report.Report;

@Options
public class StatisticsOptions {

  @Option(secure = true, name = "statistics.export", description = "write some statistics to disk")
  private boolean exportStatistics = true;

  @Option(secure = true, name = "statistics.file",
      description = "write some statistics to disk")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportStatisticsFile = Paths.get("Statistics.txt");

  @Option(secure = true, name = "statistics.print", description = "print statistics to console")
  private boolean printStatistics = false;

  @Option(secure = true, name = "pcc.proofgen.doPCC", description = "Generate and dump a proof")
  private boolean doPCC = false;

  @Option(secure = true, name = "error.export.log", description = "write error reports as plain "
      + "log file")
  @FileOption(Type.OUTPUT_FILE)
  private Path exportLogFile = Paths.get("alerts.log");

  @Option(secure = true, name = "error.export.xml", description = "write error reports as XML file")
  @FileOption(Type.OUTPUT_FILE)
  private Path exportXMLFile = Paths.get("result.xml");

  @Option(secure = true, name = "error.export.mode", description = "mode for error export")
  private Set<ErrorOutputMode> outputMode = Sets.newHashSet();

  public StatisticsOptions(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  public boolean getExportStatistics() {
    return exportStatistics;
  }

  public Path getExportStatisticsFile() {
    return exportStatisticsFile;
  }

  public boolean getPrintStatistics() {
    return printStatistics;
  }

  public boolean getDoPCC() {
    return doPCC;
  }

  public void outputErrorReport() throws Exception {
    if (outputMode.contains(LOG)) {
      BugSummary summary = (BugSummary) GlobalInfo.getInstance().exportErrorForLog();
      String log = summary.toString();
      File logFile = exportLogFile.toFile();
      BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
      writer.write(log);
      writer.newLine();
      writer.close();
    }
    if (outputMode.contains(XML)) {
      Report report = (Report) GlobalInfo.getInstance().exportErrorForReport();
      Serializer serializer = new Persister();
      File xmlFile = exportXMLFile.toFile();
      serializer.write(report, xmlFile);
    }
    GlobalInfo.getInstance().resetBugCollector();
  }

  public enum ErrorOutputMode {
    LOG,
    XML
  }

}
