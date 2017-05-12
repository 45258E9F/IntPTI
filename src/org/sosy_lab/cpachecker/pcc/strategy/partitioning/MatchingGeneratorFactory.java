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
package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.pcc.MatchingGenerator;

public class MatchingGeneratorFactory {

  private MatchingGeneratorFactory() {
  }

  public static enum MatchingGenerators {
    RANDOM,
    HEAVY_EDGE
  }

  public static MatchingGenerator createMatchingGenerator(
      final LogManager pLogger,
      MatchingGenerators generator) {
    switch (generator) {
      case HEAVY_EDGE:
        return new HeavyEdgeMatchingGenerator(pLogger);
      default:
        return new RandomMatchingGenerator(pLogger);
    }
  }
}
