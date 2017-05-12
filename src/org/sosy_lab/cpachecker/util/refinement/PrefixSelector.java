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
package org.sosy_lab.cpachecker.util.refinement;

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.VariableClassification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

public class PrefixSelector {

  private final Optional<VariableClassification> classification;
  private final Optional<LoopStructure> loopStructure;

  public InfeasiblePrefix selectSlicedPrefix(
      List<PrefixPreference> pPrefixPreference,
      List<InfeasiblePrefix> pInfeasiblePrefixes) {

    List<Comparator<InfeasiblePrefix>> comparators = createComparators(pPrefixPreference);

    TreeSet<InfeasiblePrefix> sortedPrefixes = new TreeSet<>(new ChainedComparator(comparators));
    sortedPrefixes.addAll(pInfeasiblePrefixes);

    return sortedPrefixes.first();
  }

  public int obtainScoreForPrefixes(
      final List<InfeasiblePrefix> pPrefixes,
      final PrefixPreference pPreference) {

    int minScore = Integer.MAX_VALUE;

    if (!classification.isPresent()) {
      return minScore;
    }

    Scorer scorer = new ScorerFactory(classification, loopStructure).createScorer(pPreference);

    for (InfeasiblePrefix prefix : pPrefixes) {
      minScore = Math.min(minScore, scorer.computeScore(prefix));
    }

    return minScore;
  }

  private List<Comparator<InfeasiblePrefix>> createComparators(List<PrefixPreference> pPrefixPreference) {

    ScorerFactory factory = new ScorerFactory(classification, loopStructure);

    List<Comparator<InfeasiblePrefix>> comparators = new ArrayList<>();
    for (PrefixPreference preference : pPrefixPreference) {
      comparators.add(factory.createScorer(preference).getComparator());
    }

    return comparators;
  }

  private static class ChainedComparator implements Comparator<InfeasiblePrefix>, Serializable {

    private static final long serialVersionUID = -6359291861139423226L;

    private final List<Comparator<InfeasiblePrefix>> comparators;

    public ChainedComparator(final List<Comparator<InfeasiblePrefix>> pComparators) {
      comparators = pComparators;
    }

    @Override
    public int compare(final InfeasiblePrefix onePrefix, final InfeasiblePrefix otherPrefix) {
      for (Comparator<InfeasiblePrefix> comparator : comparators) {
        int result = comparator.compare(onePrefix, otherPrefix);

        if (result != 0) {
          return result;
        }
      }

      return 0;
    }
  }

  public static final List<PrefixPreference> NO_SELECTION =
      Collections.singletonList(PrefixPreference.NONE);

  public enum PrefixPreference {

    // heuristics based on the length of the infeasible prefix
    LENGTH_MIN,
    LENGTH_MAX,

    // heuristics based on domain-type score over variables in the interpolant sequence
    DOMAIN_MIN,
    DOMAIN_MAX,

    // heuristics based on loop-counter variables referenced in the interpolant sequence
    LOOPS_MIN,
    LOOPS_MAX,

    // heuristics based on approximating the depth of the refinement root
    PIVOT_MIN,
    PIVOT_MAX,

    // heuristic based on the width of the interpolant sequence
    WIDTH_MIN,
    WIDTH_MAX,

    // heuristic based on counting the number of assignments over variables in the interpolant sequence
    ASSIGNMENTS_MIN,
    ASSIGNMENTS_MAX,

    // heuristic based on counting the number of assumptions over variables in the interpolant sequence
    ASSUMPTIONS_MIN,
    ASSUMPTIONS_MAX,

    // use this only if you are feeling lucky
    RANDOM,

    // signals to not perform any selection
    NONE
  }

  private static class ScorerFactory {

    private final Optional<VariableClassification> classification;
    private final Optional<LoopStructure> loopStructure;

    public ScorerFactory(
        final Optional<VariableClassification> pClassification,
        final Optional<LoopStructure> pLoopStructure) {
      classification = pClassification;
      loopStructure = pLoopStructure;
    }

    public Scorer createScorer(PrefixPreference pPreference) {
      switch (pPreference) {
        case LENGTH_MIN:
          return new LengthScorer();
        case LENGTH_MAX:
          return new LengthScorer().invert();
        case DOMAIN_MIN:
          return new DomainScorer(classification, loopStructure);
        case DOMAIN_MAX:
          return new DomainScorer(classification, loopStructure).invert();
        case LOOPS_MIN:
          return new LoopScorer(classification, loopStructure);
        case LOOPS_MAX:
          return new LoopScorer(classification, loopStructure).invert();
        case WIDTH_MIN:
          return new WidthScorer();
        case WIDTH_MAX:
          return new WidthScorer().invert();
        case PIVOT_MIN:
          return new DepthScorer();
        case PIVOT_MAX:
          return new DepthScorer().invert();
        case ASSIGNMENTS_MIN:
          return new AssignmentScorer(classification);
        case ASSIGNMENTS_MAX:
          return new AssignmentScorer(classification).invert();
        case ASSUMPTIONS_MIN:
          return new AssumptionScorer(classification);
        case ASSUMPTIONS_MAX:
          return new AssumptionScorer(classification).invert();
        case RANDOM:
          return new RandomScorer();

        // illegal arguments
        case NONE:
        default:
          throw new IllegalArgumentException(
              "Illegal prefix preference " + pPreference + " given!");
      }
    }
  }

  private abstract static class Scorer {

    protected int sign = 1;

    public abstract int computeScore(final InfeasiblePrefix pPrefix);

    public Scorer invert() {
      sign = sign * (-1);

      return this;
    }

    public Comparator<InfeasiblePrefix> getComparator() {
      return new Comparator<InfeasiblePrefix>() {

        @Override
        public int compare(InfeasiblePrefix onePrefix, InfeasiblePrefix otherPrefix) {
          return computeScore(onePrefix) - computeScore(otherPrefix);
        }
      };
    }
  }

  private static class DomainScorer extends Scorer {

    private final Optional<VariableClassification> classification;
    private final Optional<LoopStructure> loopStructure;

    public DomainScorer(
        final Optional<VariableClassification> pClassification,
        final Optional<LoopStructure> pLoopStructure) {
      classification = pClassification;
      loopStructure = pLoopStructure;
    }

    @Override
    public int computeScore(final InfeasiblePrefix pPrefix) {
      return sign * classification.get()
          .obtainDomainTypeScoreForVariables(pPrefix.extractSetOfIdentifiers(), loopStructure);
    }
  }

  private static class LoopScorer extends Scorer {

    private final Optional<VariableClassification> classification;
    private final Optional<LoopStructure> loopStructure;

    public LoopScorer(
        final Optional<VariableClassification> pClassification,
        final Optional<LoopStructure> pLoopStructure) {
      classification = pClassification;
      loopStructure = pLoopStructure;
    }

    @Override
    public int computeScore(final InfeasiblePrefix pPrefix) {
      int score = classification.get()
          .obtainDomainTypeScoreForVariables(pPrefix.extractSetOfIdentifiers(), loopStructure);

      if (score != Integer.MAX_VALUE) {
        score = 0;
      }

      return sign * score;
    }
  }

  private static class WidthScorer extends Scorer {

    @Override
    public int computeScore(final InfeasiblePrefix pPrefix) {
      return sign * pPrefix.getNonTrivialLength();
    }
  }

  private static class DepthScorer extends Scorer {

    @Override
    public int computeScore(final InfeasiblePrefix pPrefix) {
      return sign * pPrefix.getDepthOfPivotState();
    }
  }

  private static class LengthScorer extends Scorer {

    @Override
    public int computeScore(final InfeasiblePrefix pPrefix) {
      return sign * pPrefix.getPath().size();
    }
  }

  private static class AssignmentScorer extends Scorer {

    private final Optional<VariableClassification> classification;

    public AssignmentScorer(final Optional<VariableClassification> pClassification) {
      classification = pClassification;
    }

    @Override
    public int computeScore(final InfeasiblePrefix pPrefix) {
      int count = 0;
      for (String variable : pPrefix.extractSetOfIdentifiers()) {
        count = count + classification.get().getAssignedVariables().count(variable);
      }

      return sign * count;
    }
  }

  private static class AssumptionScorer extends Scorer {

    private final Optional<VariableClassification> classification;

    public AssumptionScorer(final Optional<VariableClassification> pClassification) {
      classification = pClassification;
    }

    @Override
    public int computeScore(final InfeasiblePrefix pPrefix) {
      int count = 0;
      for (String variable : pPrefix.extractSetOfIdentifiers()) {
        count = count + classification.get().getAssumedVariables().count(variable);
      }

      return sign * count;
    }
  }

  private static class RandomScorer extends Scorer {

    @Override
    public int computeScore(final InfeasiblePrefix pPrefix) {
      return new Random().nextInt(1000);
    }
  }

  public PrefixSelector(
      Optional<VariableClassification> pClassification,
      Optional<LoopStructure> pLoopStructure) {
    classification = pClassification;
    loopStructure = pLoopStructure;
  }
}
