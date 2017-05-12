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
package org.sosy_lab.cpachecker.core.defaults;


import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * This class implements the PrecisionAdjustment operator for a CPA, where the
 * precision never changes. It does not make any assumptions about the precision,
 * even not that the precision is non-null.
 */
public class StaticPrecisionAdjustment extends SimplePrecisionAdjustment {

  private StaticPrecisionAdjustment() {
  }

  @Override
  public Action prec(AbstractState pState, Precision pPrecision) throws CPAException {
    return Action.CONTINUE;
  }

  private static final PrecisionAdjustment instance = new StaticPrecisionAdjustment();

  public static PrecisionAdjustment getInstance() {
    return instance;
  }
}
