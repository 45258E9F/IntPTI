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
package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

import java.io.PrintStream;

import javax.annotation.Nullable;

/**
 * A class to output statistics and results of an analysis.
 *
 * You usually want to implement {@link StatisticsProvider} and register your
 * Statistics instances so that they are actually called after CPAchecker finishes.
 */
public interface Statistics {

  /**
   * Prints this group of statistics using the given PrintStream.
   *
   * This is also the correct place to write any output files the user may wish
   * to the disk. Please add a configuration option of the following form
   * in order to determine the file name for output files:
   * <code>
   *
   * @param out     the PrintStream to use for printing the statistics
   * @param result  the result of the analysis
   * @param reached the final reached set
   * @Option(secure=true, description="...", name="...)
   * @FileOption(FileOption.Type.OUTPUT_FILE) private File outputFile = new File("Default
   * Filename.txt"); </code> Note that <code>outputFile</code> may be null because the user disabled
   * output files (do not write anything in this case). Do not forget to obtain a {@link
   * org.sosy_lab.common.configuration.Configuration} instance and call <code>inject(this)</code> in
   * your constructor as usual.
   */
  public void printStatistics(PrintStream out, Result result, ReachedSet reached);

  /**
   * Define a name for this group of statistics.
   * May be null, in this case no headings is printed and
   * {@link #printStatistics(PrintStream, Result, ReachedSet)}
   * should not actually write to the PrintStream
   * (but may still write output files for example).
   *
   * @return A String with a human-readable name or null.
   */
  public
  @Nullable
  String getName();
}
