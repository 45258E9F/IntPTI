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

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;


public class SingleSignInIntervalChecker extends PerElementPropertyChecker {

  private final SingleSignChecker signChecker;
  private final InIntervalChecker intervalChecker;

  public SingleSignInIntervalChecker(
      final String pLabel, final String pVarName,
      final String pSignVal, final String pIntervalMode, final String pIntervalBound) {
    signChecker = new SingleSignChecker(pVarName, pSignVal, pLabel);
    intervalChecker = new InIntervalChecker(pVarName, pLabel, pIntervalMode, pIntervalBound);
  }

  public SingleSignInIntervalChecker(
      final String pLabel,
      final String pVarName,
      final String pSignVal,
      final String pIntervalMode,
      final String pMinBound,
      final String pMaxBound) {
    signChecker = new SingleSignChecker(pVarName, pSignVal, pLabel);
    intervalChecker = new InIntervalChecker(pVarName, pLabel, pIntervalMode, pMinBound, pMaxBound);
  }


  @Override
  public boolean satisfiesProperty(AbstractState pElemToCheck)
      throws UnsupportedOperationException {
    return signChecker.satisfiesProperty(pElemToCheck) && intervalChecker
        .satisfiesProperty(pElemToCheck);
  }
}
