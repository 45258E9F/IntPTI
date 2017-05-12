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
package org.sosy_lab.cpachecker.cpa.value.refiner.utils;

import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.util.refinement.InterpolantManager;

/**
 * InterpolantManager for interpolants of {@link ValueAnalysisState}.
 */
public class ValueAnalysisInterpolantManager
    implements InterpolantManager<ValueAnalysisState, ValueAnalysisInterpolant> {

  private static final ValueAnalysisInterpolantManager SINGLETON =
      new ValueAnalysisInterpolantManager();

  private ValueAnalysisInterpolantManager() {
    // DO NOTHING
  }

  public static ValueAnalysisInterpolantManager getInstance() {
    return SINGLETON;
  }

  @Override
  public ValueAnalysisInterpolant createInitialInterpolant() {
    return ValueAnalysisInterpolant.createInitial();
  }

  @Override
  public ValueAnalysisInterpolant createInterpolant(ValueAnalysisState state) {
    return state.createInterpolant();
  }

  @Override
  public ValueAnalysisInterpolant getTrueInterpolant() {
    return ValueAnalysisInterpolant.TRUE;
  }

  @Override
  public ValueAnalysisInterpolant getFalseInterpolant() {
    return ValueAnalysisInterpolant.FALSE;
  }
}
