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
package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.interpolant;

import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.constraints.util.ConstraintsInformation;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.ForgettingCompositeState;
import org.sosy_lab.cpachecker.util.refinement.InterpolantManager;

/**
 * Manager for {@link SymbolicInterpolant}.
 */
public class SymbolicInterpolantManager
    implements InterpolantManager<ForgettingCompositeState, SymbolicInterpolant> {

  private static final SymbolicInterpolantManager SINGLETON = new SymbolicInterpolantManager();

  public static SymbolicInterpolantManager getInstance() {
    return SINGLETON;
  }

  private SymbolicInterpolantManager() {
    // DO NOTHING
  }

  @Override
  public SymbolicInterpolant createInitialInterpolant() {
    return SymbolicInterpolant.TRUE;
  }

  @Override
  public SymbolicInterpolant createInterpolant(ForgettingCompositeState state) {
    final ValueAnalysisState values = state.getValueState();
    final ConstraintsState constraints = state.getConstraintsState();

    return new SymbolicInterpolant(values.getInformation(),
        new ConstraintsInformation(constraints, constraints.getDefiniteAssignment()));
  }

  @Override
  public SymbolicInterpolant getTrueInterpolant() {
    return SymbolicInterpolant.TRUE;
  }

  @Override
  public SymbolicInterpolant getFalseInterpolant() {
    return SymbolicInterpolant.FALSE;
  }
}
