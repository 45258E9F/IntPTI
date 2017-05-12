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
package org.sosy_lab.cpachecker.cpa.statistics;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * Represents Data of an StatisticsProvider.
 * The data provider takes care of tracking the current value, calculating the next value
 * and merging paths.
 * All instances of this interface should be immutable.
 * (Create new StatisticsDataProvider instances for new data)
 */
public interface StatisticsDataProvider {
  Object getPropertyValue();

  StatisticsDataProvider calculateNext(CFAEdge node);

  StatisticsDataProvider mergePath(StatisticsDataProvider other);
}
