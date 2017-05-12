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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public abstract class AbstractLocationFormulaInvariant implements LocationFormulaInvariant {

  private final Set<CFANode> locations;

  public AbstractLocationFormulaInvariant(CFANode pLocation) {
    Preconditions.checkNotNull(pLocation);
    this.locations = Collections.singleton(pLocation);
  }

  public AbstractLocationFormulaInvariant(Set<? extends CFANode> pLocations) {
    Preconditions.checkNotNull(pLocations);
    this.locations = ImmutableSet.copyOf(pLocations);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.algorithm.bmc.LocationInvariant#getLocations()
   */
  @Override
  public Set<CFANode> getLocations() {
    return locations;
  }

  @Override
  public BooleanFormula getAssertion(
      Iterable<AbstractState> pReachedSet,
      FormulaManagerView pFMGR,
      PathFormulaManager pPFMGR,
      int pDefaultIndex)
      throws CPATransferException, InterruptedException {
    Iterable<AbstractState> locationStates = AbstractStates.filterLocations(pReachedSet, locations);
    FluentIterable<BooleanFormula> assertions =
        FluentIterable.from(BMCHelper.assertAt(locationStates, this, pFMGR, pPFMGR, pDefaultIndex));
    return pFMGR.getBooleanFormulaManager().and(assertions.toList());
  }

  @Override
  public void assumeTruth(ReachedSet pReachedSet) {
    // Do nothing
  }

  @Override
  public void attemptInjection(InvariantGenerator pInvariantGenerator)
      throws UnrecognizedCodeException {
    // Do nothing
  }

  public static LocationFormulaInvariant makeBooleanInvariant(
      CFANode pLocation,
      final boolean pValue) {
    return new AbstractLocationFormulaInvariant(pLocation) {

      @Override
      public BooleanFormula getFormula(
          FormulaManagerView pFMGR, PathFormulaManager pPFMGR, PathFormula pContext)
          throws CPATransferException, InterruptedException {
        return pFMGR.getBooleanFormulaManager().makeBoolean(pValue);
      }
    };
  }

  public static AbstractLocationFormulaInvariant makeLocationInvariant(
      final CFANode pLocation, final String pInvariant) {
    return new AbstractLocationFormulaInvariant(pLocation) {

      /**
       * Is the invariant known to be the boolean constant 'false'
       */
      private boolean isDefinitelyBooleanFalse = false;
      private final Map<FormulaManagerView, BooleanFormula> cachedFormulas = new HashMap<>();

      @Override
      public BooleanFormula getFormula(
          FormulaManagerView pFMGR, PathFormulaManager pPFMGR, PathFormula pContext)
          throws CPATransferException, InterruptedException {
        BooleanFormula formula;

        if (cachedFormulas.containsKey(pFMGR)) {
          formula = cachedFormulas.get(pFMGR);
        } else {
          formula = pFMGR.parse(pInvariant);
          cachedFormulas.put(pFMGR, formula);
        }

        if (!isDefinitelyBooleanFalse && pFMGR.getBooleanFormulaManager().isFalse(formula)) {
          isDefinitelyBooleanFalse = true;
        }
        return formula;
      }

      @Override
      public String toString() {
        return pInvariant;
      }

      @Override
      public void assumeTruth(ReachedSet pReachedSet) {
        if (isDefinitelyBooleanFalse) {
          Iterable<AbstractState> targetStates =
              Lists.newArrayList(AbstractStates.filterLocation(pReachedSet, pLocation));
          pReachedSet.removeAll(targetStates);
          for (ARGState s : from(targetStates).filter(ARGState.class)) {
            s.removeFromARG();
          }
        }
      }
    };
  }
}
