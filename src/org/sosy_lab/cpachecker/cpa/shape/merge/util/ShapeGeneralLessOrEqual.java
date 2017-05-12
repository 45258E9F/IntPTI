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
package org.sosy_lab.cpachecker.cpa.shape.merge.util;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

public final class ShapeGeneralLessOrEqual {

  private static ShapeGeneralLessOrEqual instance;

  private final ShapeLessOrEqual lessOrEqual;

  private ShapeGeneralLessOrEqual(String pMergeType, Configuration pConfig, Solver pSolver)
      throws InvalidConfigurationException {
    if (pMergeType.equals("JOIN")) {
      lessOrEqual = new ShapeComplexLessOrEqual(pConfig, pSolver);
    } else {
      // including SEP-merge
      lessOrEqual = new ShapeTrivialLessOrEqual();
    }
  }

  public static ShapeGeneralLessOrEqual getInstance() {
    if (instance == null) {
      throw new UnsupportedOperationException("ShapeLessOrEqual should be initialized first");
    }
    return instance;
  }

  public static void initialize(String pMergeType, Configuration pConfig, Solver pSolver)
      throws InvalidConfigurationException {
    instance = new ShapeGeneralLessOrEqual(pMergeType, pConfig, pSolver);
  }

  public ShapeLessOrEqual getLessOrEqual() {
    return lessOrEqual;
  }

}
