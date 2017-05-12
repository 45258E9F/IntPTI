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
package org.sosy_lab.cpachecker.util.predicates.interpolation;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Model.ValueAssignment;

import java.util.List;
import java.util.Map;


/**
 * A class that stores information about a counterexample trace.
 * For spurious counterexamples, this stores the interpolants.
 */
public class CounterexampleTraceInfo {
  private final boolean spurious;
  private final ImmutableList<BooleanFormula> interpolants;
  private final ImmutableList<ValueAssignment> mCounterexampleModel;
  private final ImmutableList<BooleanFormula> mCounterexampleFormula;
  private final ImmutableMap<Integer, Boolean> branchingPreds;

  private CounterexampleTraceInfo(
      boolean pSpurious,
      ImmutableList<BooleanFormula> pInterpolants,
      ImmutableList<ValueAssignment> pCounterexampleModel,
      ImmutableList<BooleanFormula> pCounterexampleFormula,
      ImmutableMap<Integer, Boolean> pBranchingPreds) {
    spurious = pSpurious;
    interpolants = pInterpolants;
    mCounterexampleModel = pCounterexampleModel;
    mCounterexampleFormula = pCounterexampleFormula;
    branchingPreds = pBranchingPreds;
  }

  public static CounterexampleTraceInfo infeasible(List<BooleanFormula> pInterpolants) {
    return new CounterexampleTraceInfo(true,
        ImmutableList.copyOf(pInterpolants),
        null,
        ImmutableList.<BooleanFormula>of(),
        ImmutableMap.<Integer, Boolean>of()
    );
  }

  public static CounterexampleTraceInfo infeasibleNoItp() {
    return new CounterexampleTraceInfo(true,
        null,
        null,
        ImmutableList.<BooleanFormula>of(),
        ImmutableMap.<Integer, Boolean>of()
    );
  }

  public static CounterexampleTraceInfo feasible(
      List<BooleanFormula> pCounterexampleFormula,
      Iterable<ValueAssignment> pModel,
      Map<Integer, Boolean> preds) {
    return new CounterexampleTraceInfo(false,
        ImmutableList.<BooleanFormula>of(),
        ImmutableList.copyOf(pModel),
        ImmutableList.copyOf(pCounterexampleFormula),
        ImmutableMap.copyOf(preds)
    );
  }

  /**
   * checks whether this trace is a real bug or a spurious counterexample
   *
   * @return true if this trace is spurious, false otherwise
   */
  public boolean isSpurious() {
    return spurious;
  }

  /**
   * Returns the list of interpolants that were discovered during
   * counterexample analysis.
   *
   * @return a list of interpolants
   */
  public List<BooleanFormula> getInterpolants() {
    checkState(spurious);
    return interpolants;
  }

  @Override
  public String toString() {
    return "Spurious: " + isSpurious() +
        (isSpurious() ? ", interpolants: " + interpolants : "");
  }

  public List<BooleanFormula> getCounterExampleFormulas() {
    checkState(!spurious);
    return mCounterexampleFormula;
  }

  public ImmutableList<ValueAssignment> getModel() {
    checkState(!spurious);
    return mCounterexampleModel;
  }

  public Map<Integer, Boolean> getBranchingPredicates() {
    checkState(!spurious);
    return branchingPreds;
  }
}
