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
package org.sosy_lab.cpachecker.core.interfaces;

import java.util.Collection;

/**
 * Interface for classes that provide statistics.
 * This can be CPAs and algorithms.
 */
public interface StatisticsProvider {

  /**
   * Add a {@link Statistics} object from this provider to a collection.
   *
   * The provider is free to add zero, one or more objects. However it SHOULD not
   * make any other modifications to the collection.
   *
   * @param statsCollection The collection where the statistics are added.
   */
  public void collectStatistics(Collection<Statistics> statsCollection);

}
