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
package org.sosy_lab.cpachecker.cpa.statistics;

/**
 * A StatisticsProvider is providing some kind of Statistics for the StatisticsCPA.
 * For example the function-call-count or some other metric.
 * The data is tracked in an StatisticsDataProvider instance.
 * All instances of this interface should be immutable (there should be no need for mutable state).
 * Instances of this class are used in a HashMap and
 * should therefore prefer to not implement hashCode if you
 * want to be able to use multiple instances of this Provider.
 */
public interface StatisticsProvider {
  /**
   * The name of the metric this provider provides.
   */
  String getPropertyName();

  /**
   * The type of merging this provider is configured for
   */
  String getMergeType();

  /**
   * Creates an initial State with some initial data for the metric.
   */
  StatisticsDataProvider createDataProvider();
}
