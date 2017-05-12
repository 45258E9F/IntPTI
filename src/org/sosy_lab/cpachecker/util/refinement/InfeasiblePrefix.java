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
package org.sosy_lab.cpachecker.util.refinement;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class InfeasiblePrefix {

  private final ARGPath prefix;

  private final List<Set<String>> interpolantSequence;

  private final List<BooleanFormula> pathFormulas;

  private InfeasiblePrefix(
      final ARGPath pInfeasiblePrefix,
      final List<Set<String>> pSimpleInterpolantSequence) {

    prefix = pInfeasiblePrefix;
    interpolantSequence = pSimpleInterpolantSequence;

    pathFormulas = null;
  }

  private InfeasiblePrefix(
      final ARGPath pInfeasiblePrefix,
      final List<Set<String>> pSimpleInterpolantSequence,
      final List<BooleanFormula> pPathFormulas) {

    prefix = pInfeasiblePrefix;
    interpolantSequence = pSimpleInterpolantSequence;

    pathFormulas = pPathFormulas;
  }

  public static InfeasiblePrefix buildForPredicateDomain(
      final RawInfeasiblePrefix pRawInfeasiblePrefix,
      final FormulaManagerView pFmgr) {

    List<Set<String>> simpleInterpolantSequence = new ArrayList<>();
    for (BooleanFormula itp : pRawInfeasiblePrefix.interpolantSequence) {
      simpleInterpolantSequence.add(pFmgr.extractVariableNames(pFmgr.uninstantiate(itp)));
    }

    return new InfeasiblePrefix(pRawInfeasiblePrefix.prefix,
        simpleInterpolantSequence,
        pRawInfeasiblePrefix.pathFormulas);
  }

  public static InfeasiblePrefix buildForValueDomain(
      final ARGPath pInfeasiblePrefix,
      final List<ValueAnalysisInterpolant> pInterpolantSequence) {

    List<Set<String>> simpleInterpolantSequence = new ArrayList<>();
    for (ValueAnalysisInterpolant itp : pInterpolantSequence) {
      simpleInterpolantSequence.add(FluentIterable.from(itp.getMemoryLocations())
          .transform(MemoryLocation.FROM_MEMORYLOCATION_TO_STRING).toSet());
    }

    return new InfeasiblePrefix(pInfeasiblePrefix, simpleInterpolantSequence);
  }

  public Set<String> extractSetOfIdentifiers() {
    return FluentIterable.from(interpolantSequence)
        .transformAndConcat(new Function<Set<String>, Iterable<String>>() {
          @Override
          public Iterable<String> apply(Set<String> itp) {
            return itp;
          }
        }).toSet();
  }

  public int getNonTrivialLength() {
    return FluentIterable.from(interpolantSequence).filter(new Predicate<Set<String>>() {
      @Override
      public boolean apply(Set<String> pInput) {
        return !pInput.isEmpty();
      }
    }).size();
  }

  public int getDepthOfPivotState() {
    int depth = 0;

    for (Set<String> itp : interpolantSequence) {
      if (!itp.isEmpty()) {
        return depth;
      }

      depth++;
    }
    assert false : "There must be at least one trivial interpolant along the prefix";

    return -1;
  }

  public ARGPath getPath() {
    return prefix;
  }

  public List<BooleanFormula> getPathFormulae() {
    return pathFormulas;
  }

  public static class RawInfeasiblePrefix {

    private final ARGPath prefix;
    private final List<BooleanFormula> interpolantSequence;
    private final List<BooleanFormula> pathFormulas;

    public RawInfeasiblePrefix(
        final ARGPath pInfeasiblePrefix,
        final List<BooleanFormula> pInterpolantSequence,
        final List<BooleanFormula> pPathFormulas) {

      this.prefix = pInfeasiblePrefix;
      this.interpolantSequence = pInterpolantSequence;
      this.pathFormulas = pPathFormulas;
    }
  }
}
