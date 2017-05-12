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
package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import org.sosy_lab.cpachecker.core.interfaces.pcc.FiducciaMattheysesOptimizer;

public class FiducciaMattheysesOptimzerFactory {

  private FiducciaMattheysesOptimzerFactory() {
  }

  public static enum OptimizationCriteria {
    EDGECUT,
    NODECUT
  }

  public static FiducciaMattheysesOptimizer createFMOptimizer(OptimizationCriteria criterion) {
    switch (criterion) {
      case EDGECUT:
        return new EdgeCutOptimizer();
      default:
        return new NodeCutOptimizer();
    }
  }
}
