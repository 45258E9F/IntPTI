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
package org.sosy_lab.cpachecker.pcc.propertychecker;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.interval.Interval;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisState;
import org.sosy_lab.cpachecker.util.AbstractStates;


public class InIntervalChecker extends PerElementPropertyChecker {

  private final String label;
  private final String varName;
  private final Interval allowedValues;

  public InIntervalChecker(
      final String pVariableName, final String pLabel, final String pMode, final String pMin,
      final String pMax) {
    label = pLabel;
    varName = pVariableName;
    allowedValues = new Interval(Long.parseLong(pMin), Long.parseLong(pMax));

  }

  public InIntervalChecker(
      final String pVariableName,
      final String pLabel,
      final String pMode,
      final String pValue) {
    this(pVariableName, pLabel, pMode,
        Integer.parseInt(pMode) == 0 ? pValue : Long.toString(Long.MIN_VALUE),
        Integer.parseInt(pMode) == 0 ? Long.toString(Long.MAX_VALUE) : pValue);
  }

  @Override
  public boolean satisfiesProperty(AbstractState pElemToCheck)
      throws UnsupportedOperationException {
    CFANode node = AbstractStates.extractLocation(pElemToCheck);
    if (node instanceof CLabelNode && ((CLabelNode) node).getLabel().equals(label)) {
      IntervalAnalysisState state =
          AbstractStates.extractStateByType(pElemToCheck, IntervalAnalysisState.class);
      if (state != null) {
        Interval interval = state.getInterval(varName);
        if (interval != null && interval.getHigh() <= allowedValues.getHigh()
            && interval.getLow() >= allowedValues.getLow()) {
          return true;
        }
      }
      return false;
    }
    return true;
  }

}
