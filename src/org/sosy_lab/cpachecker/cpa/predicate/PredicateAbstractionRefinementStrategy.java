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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision.mergePredicatesPerFunction;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision.mergePredicatesPerLocation;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateMapWriter;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.FormulaMeasuring;
import org.sosy_lab.cpachecker.util.predicates.FormulaMeasuring.FormulaMeasures;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.solver.api.BooleanFormula;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * This class provides the refinement strategy for the classical predicate
 * abstraction (adding the predicates from the interpolant to the precision
 * and removing the relevant parts of the ARG).
 */
@Options(prefix = "cpa.predicate")
public class PredicateAbstractionRefinementStrategy extends RefinementStrategy {

  @Option(secure = true, name = "precision.sharing",
      description = "Where to apply the found predicates to?")
  private PredicateSharing predicateSharing = PredicateSharing.LOCATION;

  private static enum PredicateSharing {
    GLOBAL,
    // at all locations
    SCOPE,
    // at all locations in the scope of the variable
    FUNCTION,
    // at all locations in the respective function
    LOCATION,
    // at all occurrences of the respective location
    LOCATION_INSTANCE,
    // at the n-th occurrence of the respective location in each path
    ;
  }

  @Option(secure = true, name = "refinement.predicateBasisStrategy",
      description = "Which predicates should be used as basis for a new precision."
          + "ALL: During refinement, keep predicates from all removed parts of the ARG."
          + "CUTPOINT: Only predicates from the cut-point's precision are kept."
          + "TARGET: Only predicates from the target state's precision are kept.")
  /* There are usually more predicates at the target location that at the cut-point.
   * An evaluation on 4000 source files for ALL, TARGET, and CUTPOINT showed:
   * - (nearly) no difference for predicate analysis L.
   * - predicate analysis LF is much slower with CUTPOINT than with TARGET or ALL
   *   (especially on the source files product-lines/minepump_spec*).
   */
  private PredicateBasisStrategy predicateBasisStrategy = PredicateBasisStrategy.TARGET;

  private static enum PredicateBasisStrategy {
    ALL,
    TARGET,
    CUTPOINT
  }

  @Option(secure = true, name = "refinement.restartAfterRefinements",
      description = "Do a complete restart (clearing the reached set) "
          + "after N refinements. 0 to disable, 1 for always.")
  @IntegerOption(min = 0)
  private int restartAfterRefinements = 0;

  @Option(secure = true, name = "refinement.sharePredicates",
      description = "During refinement, add all new predicates to the precisions "
          + "of all abstract states in the reached set.")
  private boolean sharePredicates = false;

  @Option(secure = true, name = "refinement.useBddInterpolantSimplification",
      description = "Use BDDs to simplify interpolants "
          + "(removing irrelevant predicates)")
  private boolean useBddInterpolantSimplification = false;

  @Option(secure = true, name = "refinement.dumpPredicates",
      description = "After each refinement, dump the newly found predicates.")
  private boolean dumpPredicates = false;

  @Option(secure = true, name = "refinement.dumpPredicatesFile",
      description = "File name for the predicates dumped after refinements.")
  @FileOption(Type.OUTPUT_FILE)
  private PathTemplate dumpPredicatesFile =
      PathTemplate.ofFormatString("refinement%04d-predicates.prec");

  private int refinementCount = 0; // this is modulo restartAfterRefinements

  private boolean atomicPredicates = false;

  protected final LogManager logger;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final PredicateAbstractionManager predAbsMgr;
  private final FormulaMeasuring formulaMeasuring;
  private final PredicateMapWriter precisionWriter;

  // statistics
  private StatCounter numberOfRefinementsWithStrategy2 =
      new StatCounter("Number of refs with location-based cutoff");
  private StatInt irrelevantPredsInItp =
      new StatInt(StatKind.SUM, "Number of irrelevant preds in interpolants");

  private StatTimer predicateCreation = new StatTimer(StatKind.SUM, "Predicate creation");
  private StatTimer precisionUpdate = new StatTimer(StatKind.SUM, "Precision update");
  private StatTimer argUpdate = new StatTimer(StatKind.SUM, "ARG update");
  private StatTimer itpSimplification = new StatTimer(StatKind.SUM, "Itp simplification with BDDs");

  private StatInt simplifyDeltaConjunctions = new StatInt(StatKind.SUM, "Conjunctions Delta");
  private StatInt simplifyDeltaDisjunctions = new StatInt(StatKind.SUM, "Disjunctions Delta");
  private StatInt simplifyDeltaNegations = new StatInt(StatKind.SUM, "Negations Delta");
  private StatInt simplifyDeltaAtoms = new StatInt(StatKind.SUM, "Atoms Delta");
  private StatInt simplifyDeltaVariables = new StatInt(StatKind.SUM, "Variables Delta");
  private StatInt simplifyVariablesBefore = new StatInt(StatKind.SUM, "Variables Before");
  private StatInt simplifyVariablesAfter = new StatInt(StatKind.SUM, "Variables After");

  private class Stats implements Statistics {
    @Override
    public String getName() {
      return "Predicate-Abstraction Refiner";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      StatisticsWriter w0 = StatisticsWriter.writingStatisticsTo(out);
      StatisticsWriter w1 = w0.beginLevel();

      w1.put(predicateCreation)
          .ifUpdatedAtLeastOnce(itpSimplification)
          .put(itpSimplification)
          .beginLevel()
          .put(simplifyDeltaConjunctions)
          .put(simplifyDeltaDisjunctions)
          .put(simplifyDeltaNegations)
          .put(simplifyDeltaAtoms)
          .put(simplifyDeltaVariables)
          .put(simplifyVariablesBefore)
          .put(simplifyVariablesAfter);

      w1.put(precisionUpdate)
          .put(argUpdate)
          .spacer();

      basicRefinementStatistics.printStatistics(out, pResult, pReached);

      w0.put(numberOfRefinementsWithStrategy2)
          .ifUpdatedAtLeastOnce(itpSimplification)
          .put(irrelevantPredsInItp);
    }
  }

  public PredicateAbstractionRefinementStrategy(
      final Configuration config,
      final LogManager pLogger,
      final PredicateAbstractionManager pPredAbsMgr,
      final Solver pSolver)
      throws InvalidConfigurationException {
    super(pSolver);

    config.inject(this, PredicateAbstractionRefinementStrategy.class);

    logger = pLogger;
    fmgr = pSolver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    predAbsMgr = pPredAbsMgr;
    formulaMeasuring = new FormulaMeasuring(fmgr);

    if (dumpPredicates && dumpPredicatesFile != null) {
      precisionWriter = new PredicateMapWriter(config, fmgr);
    } else {
      precisionWriter = null;
    }
  }

  private ListMultimap<Pair<CFANode, Integer>, AbstractionPredicate> newPredicates;


  final void setUseAtomicPredicates(boolean atomicPredicates) {
    this.atomicPredicates = atomicPredicates;
  }

  @Override
  protected final void startRefinementOfPath() {
    checkState(newPredicates == null);
    // needs to be a fully deterministic data structure,
    // thus a Multimap based on a LinkedHashMap
    // (we iterate over the keys)
    newPredicates = MultimapBuilder.linkedHashKeys().arrayListValues().build();
  }

  @Override
  protected final boolean performRefinementForState(
      BooleanFormula pInterpolant,
      ARGState interpolationPoint) {
    checkState(newPredicates != null);
    checkArgument(!bfmgr.isTrue(pInterpolant));

    predicateCreation.start();
    PredicateAbstractState predicateState = getPredicateState(interpolationPoint);
    PathFormula blockFormula = predicateState.getAbstractionFormula().getBlockFormula();

    Collection<AbstractionPredicate> localPreds = convertInterpolant(pInterpolant, blockFormula);
    CFANode loc = AbstractStates.extractLocation(interpolationPoint);
    int locInstance = predicateState.getAbstractionLocationsOnPath().get(loc);

    newPredicates.putAll(Pair.of(loc, locInstance), localPreds);
    predicateCreation.stop();

    return false;
  }

  /**
   * Get the predicates out of an interpolant.
   *
   * @param pInterpolant The interpolant formula.
   * @return A set of predicates.
   */
  private final Collection<AbstractionPredicate> convertInterpolant(
      final BooleanFormula pInterpolant, PathFormula blockFormula) {

    BooleanFormula interpolant = pInterpolant;

    if (bfmgr.isTrue(interpolant)) {
      return Collections.<AbstractionPredicate>emptySet();
    }

    Collection<AbstractionPredicate> preds;

    int allPredsCount = 0;
    if (useBddInterpolantSimplification) {
      FormulaMeasures itpBeforeSimple = formulaMeasuring.measure(interpolant);

      itpSimplification.start();
      // need to call extractPredicates() for registering all predicates
      allPredsCount = predAbsMgr.getPredicatesForAtomsOf(interpolant).size();
      interpolant = predAbsMgr.buildAbstraction(fmgr.uninstantiate(interpolant), blockFormula)
          .asInstantiatedFormula();
      itpSimplification.stop();

      FormulaMeasures itpAfterSimple = formulaMeasuring.measure(interpolant);
      simplifyDeltaAtoms.setNextValue(itpAfterSimple.getAtoms() - itpBeforeSimple.getAtoms());
      simplifyDeltaDisjunctions
          .setNextValue(itpAfterSimple.getDisjunctions() - itpBeforeSimple.getDisjunctions());
      simplifyDeltaConjunctions
          .setNextValue(itpAfterSimple.getConjunctions() - itpBeforeSimple.getConjunctions());
      simplifyDeltaNegations
          .setNextValue(itpAfterSimple.getNegations() - itpBeforeSimple.getNegations());
      simplifyDeltaVariables.setNextValue(
          itpAfterSimple.getVariables().size() - itpBeforeSimple.getVariables().size());
      simplifyVariablesBefore.setNextValue(itpBeforeSimple.getVariables().size());
      simplifyVariablesAfter.setNextValue(itpAfterSimple.getVariables().size());
    }

    if (atomicPredicates) {
      preds = predAbsMgr.getPredicatesForAtomsOf(interpolant);

      if (useBddInterpolantSimplification) {
        irrelevantPredsInItp.setNextValue(allPredsCount - preds.size());
      }

    } else {
      preds = ImmutableList.of(predAbsMgr.getPredicateFor(interpolant));
    }
    assert !preds.isEmpty() : "Interpolant without relevant predicates: " + pInterpolant
        + "; simplified to " + interpolant;

    logger.log(Level.FINEST, "Got predicates", preds);

    return preds;
  }

  @Override
  protected final void finishRefinementOfPath(
      ARGState pUnreachableState,
      List<ARGState> pAffectedStates, ARGReachedSet pReached,
      boolean pRepeatedCounterexample)
      throws CPAException {

    Pair<PredicatePrecision, ARGState> newPrecAndRefinementRoot =
        computeNewPrecision(pUnreachableState, pAffectedStates, pReached, pRepeatedCounterexample);

    PredicatePrecision newPrecision = newPrecAndRefinementRoot.getFirst();
    ARGState refinementRoot = newPrecAndRefinementRoot.getSecond();

    updateARG(newPrecision, refinementRoot, pReached);

    newPredicates = null;
  }

  protected void updateARG(
      PredicatePrecision pNewPrecision, ARGState pRefinementRoot,
      ARGReachedSet pReached) {

    argUpdate.start();

    List<Precision> precisions = new ArrayList<>(2);
    List<Predicate<? super Precision>> precisionTypes = new ArrayList<>(2);

    precisions.add(pNewPrecision);
    precisionTypes.add(Predicates.instanceOf(PredicatePrecision.class));

    UnmodifiableReachedSet reached = pReached.asReachedSet();

    if (isValuePrecisionAvailable(pReached, pRefinementRoot)) {
      precisions.add(mergeAllValuePrecisionsFromSubgraph(pRefinementRoot, reached));
      precisionTypes.add(VariableTrackingPrecision.isMatchingCPAClass(ValueAnalysisCPA.class));
    }

    pReached.removeSubtree(pRefinementRoot, precisions, precisionTypes);

    assert (refinementCount > 0) || reached.size() == 1;

    if (sharePredicates) {
      pReached
          .updatePrecisionGlobally(pNewPrecision, Predicates.instanceOf(PredicatePrecision.class));
    }

    argUpdate.stop();
  }

  private final Pair<PredicatePrecision, ARGState> computeNewPrecision(
      ARGState pUnreachableState,
      List<ARGState> pAffectedStates, ARGReachedSet pReached, boolean pRepeatedCounterexample)
      throws RefinementFailedException {

    { // Add predicate "false" to unreachable location
      CFANode loc = extractLocation(pUnreachableState);
      int locInstance = getPredicateState(pUnreachableState)
          .getAbstractionLocationsOnPath().get(loc);
      newPredicates.put(Pair.of(loc, locInstance), predAbsMgr.makeFalsePredicate());
      pAffectedStates.add(pUnreachableState);
    }

    // We have two different strategies for the refinement root: set it to
    // the first interpolation point or set it to highest location in the ARG
    // where the same CFANode appears.
    // Both work, so this is a heuristics question to get the best performance.
    // My benchmark showed, that at least for the benchmarks-lbe examples it is
    // best to use strategy one iff newPredicatesFound.

    // get previous precision
    UnmodifiableReachedSet reached = pReached.asReachedSet();
    PredicatePrecision targetStatePrecision =
        extractPredicatePrecision(reached.getPrecision(reached.getLastState()));

    ARGState refinementRoot =
        getRefinementRoot(pAffectedStates, targetStatePrecision, pRepeatedCounterexample);

    logger.log(Level.FINEST, "Removing everything below", refinementRoot, "from ARG.");

    // check whether we should restart
    refinementCount++;
    if (restartAfterRefinements > 0 && refinementCount >= restartAfterRefinements) {
      ARGState root = (ARGState) reached.getFirstState();
      // we have to use the child as the refinementRoot
      assert root.getChildren().size() == 1 : "ARG root should have exactly one child";
      refinementRoot = Iterables.getLast(root.getChildren());

      logger.log(Level.FINEST, "Restarting analysis after", refinementCount,
          "refinements by clearing the ARG.");
      refinementCount = 0;
    }

    // now create new precision
    precisionUpdate.start();
    PredicatePrecision basePrecision;
    switch (predicateBasisStrategy) {
      case ALL:
        basePrecision = findAllPredicatesFromSubgraph(refinementRoot, reached);
        break;
      case TARGET:
        basePrecision = targetStatePrecision;
        break;
      case CUTPOINT:
        basePrecision = extractPredicatePrecision(reached.getPrecision(refinementRoot));
        break;
      default:
        throw new AssertionError("unknown strategy for predicate basis.");
    }

    logger.log(Level.ALL, "Old predicate map is", basePrecision);
    logger.log(Level.ALL, "New predicates are", newPredicates);

    PredicatePrecision newPrecision;
    switch (predicateSharing) {
      case GLOBAL:
        newPrecision = basePrecision.addGlobalPredicates(newPredicates.values());
        break;
      case SCOPE:
        Set<AbstractionPredicate> globalPredicates = new HashSet<>();
        ListMultimap<Pair<CFANode, Integer>, AbstractionPredicate> localPredicates =
            ArrayListMultimap.create();

        splitInLocalAndGlobalPredicates(globalPredicates, localPredicates);

        newPrecision = basePrecision.addGlobalPredicates(globalPredicates);
        newPrecision = newPrecision.addLocalPredicates(mergePredicatesPerLocation(localPredicates));

        break;
      case FUNCTION:
        newPrecision =
            basePrecision.addFunctionPredicates(mergePredicatesPerFunction(newPredicates));
        break;
      case LOCATION:
        newPrecision = basePrecision.addLocalPredicates(mergePredicatesPerLocation(newPredicates));
        break;
      case LOCATION_INSTANCE:
        newPrecision = basePrecision.addLocationInstancePredicates(newPredicates);
        break;
      default:
        throw new AssertionError();
    }

    logger.log(Level.ALL, "Predicate map now is", newPrecision);

    assert basePrecision.calculateDifferenceTo(newPrecision) == 0
        : "We forgot predicates during refinement!";
    assert targetStatePrecision.calculateDifferenceTo(newPrecision) == 0
        : "We forgot predicates during refinement!";

    if (dumpPredicates && dumpPredicatesFile != null) {
      Path precFile = dumpPredicatesFile.getPath(precisionUpdate.getUpdateCount());
      try (Writer w = Files.openOutputFile(precFile)) {
        precisionWriter.writePredicateMap(
            ImmutableSetMultimap.copyOf(newPredicates),
            ImmutableSetMultimap.<CFANode, AbstractionPredicate>of(),
            ImmutableSetMultimap.<String, AbstractionPredicate>of(),
            ImmutableSet.<AbstractionPredicate>of(),
            newPredicates.values(), w);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not dump precision to file");
      }
    }

    precisionUpdate.stop();

    return Pair.of(newPrecision, refinementRoot);
  }

  private PredicatePrecision extractPredicatePrecision(Precision oldPrecision)
      throws IllegalStateException {
    PredicatePrecision oldPredicatePrecision =
        Precisions.extractPrecisionByType(oldPrecision, PredicatePrecision.class);
    if (oldPredicatePrecision == null) {
      throw new IllegalStateException(
          "Could not find the PredicatePrecision for the error element");
    }
    return oldPredicatePrecision;
  }

  private void splitInLocalAndGlobalPredicates(
      Set<AbstractionPredicate> globalPredicates,
      ListMultimap<Pair<CFANode, Integer>, AbstractionPredicate> localPredicates) {

    for (Map.Entry<Pair<CFANode, Integer>, AbstractionPredicate> predicate : newPredicates
        .entries()) {
      if (predicate.getValue().getSymbolicAtom().toString().contains("::")) {
        localPredicates.put(predicate.getKey(), predicate.getValue());
      } else {
        globalPredicates.add(predicate.getValue());
      }
    }
  }

  private ARGState getRefinementRoot(
      List<ARGState> pAffectedStates, PredicatePrecision targetStatePrecision,
      boolean pRepeatedCounterexample) throws RefinementFailedException {
    boolean newPredicatesFound =
        !targetStatePrecision.getLocalPredicates().entries().containsAll(newPredicates.entries());

    ARGState firstInterpolationPoint = pAffectedStates.get(0);
    if (!newPredicatesFound) {
      if (pRepeatedCounterexample) {
        throw new RefinementFailedException(RefinementFailedException.Reason.RepeatedCounterexample,
            null);
      }
      numberOfRefinementsWithStrategy2.inc();

      CFANode firstInterpolationPointLocation =
          AbstractStates.extractLocation(firstInterpolationPoint);

      logger.log(Level.FINEST, "Found spurious counterexample,",
          "trying strategy 2: remove everything below node", firstInterpolationPointLocation,
          "from ARG.");

      // find top-most element in path with location == firstInterpolationPointLocation,
      // this is not necessary equal to firstInterpolationPoint
      ARGState current = firstInterpolationPoint;
      while (!current.getParents().isEmpty()) {
        current = Iterables.get(current.getParents(), 0);

        if (getPredicateState(current).isAbstractionState()) {
          CFANode loc = AbstractStates.extractLocation(current);
          if (loc.equals(firstInterpolationPointLocation)) {
            firstInterpolationPoint = current;
          }
        }
      }
    }
    return firstInterpolationPoint;
  }

  /**
   * Collect all precisions in the subgraph below refinementRoot and merge
   * their predicates.
   *
   * @return a new precision with all these predicates.
   */
  private PredicatePrecision findAllPredicatesFromSubgraph(
      ARGState refinementRoot, UnmodifiableReachedSet reached) {

    PredicatePrecision newPrecision = PredicatePrecision.empty();

    // find all distinct precisions to merge them
    Set<Precision> precisions = Sets.newIdentityHashSet();
    for (ARGState state : refinementRoot.getSubgraph()) {
      if (!state.isCovered()) {
        // covered states are not in reached set
        precisions.add(reached.getPrecision(state));
      }
    }

    for (Precision prec : precisions) {
      newPrecision = newPrecision.mergeWith(extractPredicatePrecision(prec));
    }
    return newPrecision;
  }

  private boolean isValuePrecisionAvailable(final ARGReachedSet pReached, ARGState root) {
    if (!pReached.asReachedSet().contains(root)) {
      return false;
    }
    return Precisions.extractPrecisionByType(pReached.asReachedSet().getPrecision(root),
        VariableTrackingPrecision.class) != null;
  }

  private VariableTrackingPrecision mergeAllValuePrecisionsFromSubgraph(
      ARGState refinementRoot, UnmodifiableReachedSet reached) {

    VariableTrackingPrecision rootPrecision =
        Precisions.extractPrecisionByType(reached.getPrecision(refinementRoot),
            VariableTrackingPrecision.class);

    // find all distinct precisions to merge them
    Set<Precision> precisions = Sets.newIdentityHashSet();
    for (ARGState state : refinementRoot.getSubgraph()) {
      if (!state.isCovered()) {
        // covered states are not in reached set
        precisions.add(reached.getPrecision(state));
      }
    }

    for (Precision prec : precisions) {
      rootPrecision = rootPrecision.join(Precisions.extractPrecisionByType(prec,
          VariableTrackingPrecision.class));
    }

    return rootPrecision;
  }

  @Override
  public Statistics getStatistics() {
    return new Stats();
  }
}
