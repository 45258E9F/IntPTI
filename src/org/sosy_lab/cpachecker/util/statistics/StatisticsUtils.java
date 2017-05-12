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
package org.sosy_lab.cpachecker.util.statistics;

import com.google.common.base.Strings;

import java.io.PrintStream;
import java.util.Locale;


public class StatisticsUtils {
  private StatisticsUtils() {
  }

  public static String toPercent(double val, double full) {
    return String.format("%1.0f", val / full * 100) + "%";
  }

  public static String valueWithPercentage(int value, int totalCount) {
    return value + " (" + toPercent(value, totalCount) + ")";
  }

  public static String div(double val, double full) {
    return String.format(Locale.ROOT, "%.2f", val / full);
  }

  public static void write(
      PrintStream target, int indentLevel, int outputNameColWidth,
      String name, Object value) {
    String indentation = Strings.repeat("  ", indentLevel);
    target.println(String.format("%-" + outputNameColWidth + "s %s",
        indentation + name + ":", value));
  }

  public static void write(
      PrintStream target, int indentLevel, int outputNameColWidth,
      AbstractStatValue stat) {
    write(target, indentLevel, outputNameColWidth, stat.getTitle(), stat.toString());
  }
}
