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
package org.sosy_lab.cpachecker.cpa.arg;

import org.sosy_lab.cpachecker.core.defaults.SimplePrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ARGSimplePrecisionAdjustment extends SimplePrecisionAdjustment {

  private final SimplePrecisionAdjustment wrappedPrecAdjustment;

  public ARGSimplePrecisionAdjustment(SimplePrecisionAdjustment pWrappedPrecAdjustment) {
    wrappedPrecAdjustment = pWrappedPrecAdjustment;
  }

  @Override
  public Action prec(AbstractState pElement, Precision pPrecision) throws CPAException {
    ARGState element = (ARGState) pElement;

    return wrappedPrecAdjustment.prec(element.getWrappedState(), pPrecision);
  }
}