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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.in;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.NestedTimer;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.predicate.InvariantsManager.RegionInvariantsSupplier;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateAbstractionsStorage;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateAbstractionsStorage.AbstractionNode;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateParsingFailedException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionCreator;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionCreator.RegionBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.ProverEnvironment;
import org.sosy_lab.solver.api.ProverEnvironment.AllSatCallback;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

@Options(prefix = "cpa.predicate")
public class PredicateAbstractionManager {

  static class Stats {

    public int numCallsAbstraction = 0; // total calls
    public int numAbstractionReuses = 0; // total reuses

    public int numSymbolicAbstractions = 0; // precision completely empty, no computation
    public int numSatCheckAbstractions = 0; // precision was {false}, only sat check
    public int numCallsAbstractionCached = 0; // result was cached, no computation
    public int numInductivePathFormulaCacheUsed = 0; // loop was cached, no new computation

    public int numTotalPredicates = 0;
    public int maxPredicates = 0;
    public int numIrrelevantPredicates = 0;
    public int numTrivialPredicates = 0;
    public int numInductivePredicates = 0;
    public int numCartesianAbsPredicates = 0;
    public int numCartesianAbsPredicatesCached = 0;
    public int numBooleanAbsPredicates = 0;
    public final Timer abstractionReuseTime = new Timer();
    public final StatTimer abstractionReuseImplicationTime =
        new StatTimer("Time for checking reusability of abstractions");
    public final Timer trivialPredicatesTime = new Timer();
    public final Timer inductivePredicatesTime = new Timer();
    public final Timer cartesianAbstractionTime = new Timer();
    public final Timer quantifierEliminationTime = new Timer();
    public final Timer booleanAbstractionTime = new Timer();
    public final NestedTimer abstractionEnumTime = new NestedTimer();
    // outer: solver time, inner: bdd time
    public final Timer abstractionSolveTime = new Timer();
    // only the time for solving, not for model enumeration

    public long allSatCount = 0;
    public int maxAllSatCount = 0;
  }

  final Stats stats = new Stats();

  private final LogManager logger;
  private final FormulaManagerView fmgr;
  private final AbstractionManager amgr;
  private final RegionCreator rmgr;
  private final PathFormulaManager pfmgr;
  private final Solver solver;
  private final ShutdownNotifier shutdownNotifier;

  private static final Set<Integer> noAbstractionReuse = ImmutableSet.of();

  private static enum AbstractionType {
    CARTESIAN,
    BOOLEAN,
    COMBINED,
    ELIMINATION;
  }

  @Option(secure = true, name = "abstraction.cartesian",
      description = "whether to use Boolean (false) or Cartesian (true) abstraction")
  @Deprecated
  private boolean cartesianAbstraction = false;

  @Option(secure = true, name = "abstraction.computation",
      description = "whether to use Boolean or Cartesian abstraction or both")
  private AbstractionType abstractionType = AbstractionType.BOOLEAN;

  @Option(secure = true, name = "abstraction.dumpHardQueries",
      description = "dump the abstraction formulas if they took to long")
  private boolean dumpHardAbstractions = false;

  @Option(secure = true, name = "abstraction.reuseAbstractionsFrom",
      description = "An initial set of comptued abstractions that might be reusable")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path reuseAbstractionsFrom;

  @Option(secure = true, description = "Max. number of edge of the abstraction tree to prescan for reuse")
  private int maxAbstractionReusePrescan = 1;

  @Option(secure = true, name = "abs.useCache", description = "use caching of abstractions")
  private boolean useCache = true;

  @Option(secure = true, name = "refinement.splitItpAtoms",
      description = "split each arithmetic equality into two inequalities when extracting predicates from interpolants")
  private boolean splitItpAtoms = false;

  @Option(secure = true, name = "abstraction.identifyTrivialPredicates",
      description = "Identify those predicates where the result is trivially known before abstraction computation and omit them.")
  private boolean identifyTrivialPredicates = false;

  @Option(secure = true, name = "abstraction.simplify",
      description = "Simplify the abstraction formula that is stored to represent the state space. Helpful when debugging (formulas get smaller).")
  private boolean simplifyAbstractionFormula = false;

  private boolean warnedOfCartesianAbstraction = false;

  private boolean abstractionReuseDisabledBecauseOfAmbiguity = false;

  private final Map<Pair<BooleanFormula, ImmutableSet<AbstractionPredicate>>, AbstractionFormula>
      abstractionCache;

  // Cache for satisfiability queries: if formula is contained, it is unsat
  private final Set<BooleanFormula> unsatisfiabilityCache;

  //cache for cartesian abstraction queries. For each predicate, the values
  // are -1: predicate is false, 0: predicate is don't care,
  // 1: predicate is true
  private final Map<Pair<BooleanFormula, AbstractionPredicate>, Byte> cartesianAbstractionCache;

  private final RegionInvariantsSupplier locationBasedInvariantSupplier;

  private final BooleanFormulaManagerView bfmgr;

  private final PredicateAbstractionsStorage abstractionStorage;

  private final Configuration config;

  public PredicateAbstractionManager(
      AbstractionManager pAmgr,
      PathFormulaManager pPfmgr,
      Solver pSolver,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      RegionInvariantsSupplier pRegionInvariantsSupplier)
      throws InvalidConfigurationException, PredicateParsingFailedException {
    shutdownNotifier = pShutdownNotifier;
    config = pConfig;

    config.inject(this, PredicateAbstractionManager.class);

    logger = pLogger;
    fmgr = pSolver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    amgr = pAmgr;
    rmgr = amgr.getRegionCreator();
    pfmgr = pPfmgr;
    solver = pSolver;
    locationBasedInvariantSupplier = pRegionInvariantsSupplier;

    if (cartesianAbstraction) {
      abstractionType = AbstractionType.CARTESIAN;
    }
    if (abstractionType == AbstractionType.COMBINED) {
      warnedOfCartesianAbstraction = true; // warning is not necessary
    }

    if (useCache) {
      abstractionCache = new HashMap<>();
      unsatisfiabilityCache = new HashSet<>();
    } else {
      abstractionCache = null;
      unsatisfiabilityCache = null;
    }

    if (useCache && (abstractionType != AbstractionType.BOOLEAN)) {
      cartesianAbstractionCache = new HashMap<>();
    } else {
      cartesianAbstractionCache = null;
    }

    abstractionStorage =
        new PredicateAbstractionsStorage(reuseAbstractionsFrom, logger, fmgr, null);
  }

  /**
   * Compute an abstraction of the conjunction of an AbstractionFormula and
   * a PathFormula. The AbstractionFormula will be used in its instantiated form,
   * so the indices there should match those from the PathFormula.
   *
   * @param abstractionFormula An AbstractionFormula that is used as input.
   * @param pathFormula        A PathFormula that is used as input.
   * @param pPredicates        The set of predicates used for abstraction.
   * @return An AbstractionFormula instance representing an abstraction of "abstractionFormula &
   * pathFormula" with pathFormula as the block formula.
   */
  public AbstractionFormula buildAbstraction(
      final CFANode location,
      final AbstractionFormula abstractionFormula,
      final PathFormula pathFormula,
      final Collection<AbstractionPredicate> pPredicates)
      throws SolverException, InterruptedException {

    stats.numCallsAbstraction++;

    logger.log(Level.FINEST, "Computing abstraction", stats.numCallsAbstraction, "with",
        pPredicates.size(), "predicates");
    logger.log(Level.ALL, "Old abstraction:", abstractionFormula.asFormula());
    logger.log(Level.ALL, "Path formula:", pathFormula);
    logger.log(Level.ALL, "Predicates:", pPredicates);

    final BooleanFormula absFormula = abstractionFormula.asInstantiatedFormula();
    final BooleanFormula symbFormula = buildFormula(pathFormula.getFormula());
    final BooleanFormula f = bfmgr.and(absFormula, symbFormula);
    final SSAMap ssa = pathFormula.getSsa();

    // Try to reuse stored abstractions
    if (reuseAbstractionsFrom != null
        && !abstractionReuseDisabledBecauseOfAmbiguity) {
      AbstractionFormula reused =
          reuseAbstractionIfPossible(abstractionFormula, pathFormula, f, location);
      if (reused != null) {
        return reused;
      }
    }

    // Shortcut if the precision is empty
    if (pPredicates.isEmpty() && (abstractionType != AbstractionType.ELIMINATION)) {
      logger.log(Level.FINEST, "Abstraction", stats.numCallsAbstraction,
          "with empty precision is true");
      stats.numSymbolicAbstractions++;
      return makeTrueAbstractionFormula(pathFormula);
    }

    // This is the (mutable) set of remaining predicates that still need to be handled.
    // Each step of our abstraction computation may be able to handle some predicates,
    // and should remove those from this set afterwards.
    final Collection<AbstractionPredicate> remainingPredicates =
        getRelevantPredicates(pPredicates, f, ssa, location);

    // caching
    Pair<BooleanFormula, ImmutableSet<AbstractionPredicate>> absKey = null;
    if (useCache) {
      absKey = Pair.of(f, ImmutableSet.copyOf(remainingPredicates));
      AbstractionFormula result = abstractionCache.get(absKey);

      if (result != null) {
        // create new abstraction object to have a unique abstraction id

        // instantiate the formula with the current indices
        BooleanFormula stateFormula = result.asFormula();
        BooleanFormula instantiatedFormula = fmgr.instantiate(stateFormula, ssa);

        result = new AbstractionFormula(fmgr, result.asRegion(), stateFormula,
            instantiatedFormula, pathFormula, result.getIdsOfStoredAbstractionReused());
        logger.log(Level.FINEST, "Abstraction", stats.numCallsAbstraction, "was cached");
        logger.log(Level.ALL, "Abstraction result is", result.asFormula());
        stats.numCallsAbstractionCached++;
        return result;
      }

      boolean unsatisfiable = unsatisfiabilityCache.contains(symbFormula)
          || unsatisfiabilityCache.contains(f);
      if (unsatisfiable) {
        // block is infeasible
        logger.log(Level.FINEST, "Block feasibility of abstraction", stats.numCallsAbstraction,
            "was cached and is false.");
        stats.numCallsAbstractionCached++;
        return new AbstractionFormula(fmgr, rmgr.makeFalse(),
            bfmgr.makeBoolean(false), bfmgr.makeBoolean(false),
            pathFormula, noAbstractionReuse);
      }
    }


    // We update statistics here because we want to ignore calls
    // where the result was in the cache.
    stats.numTotalPredicates += pPredicates.size();
    stats.maxPredicates = Math.max(stats.maxPredicates, pPredicates.size());
    stats.numIrrelevantPredicates += pPredicates.size() - remainingPredicates.size();

    // Compute result for those predicates
    // where we can trivially identify their truthness in the result
    Region abs = rmgr.makeTrue();
    if (identifyTrivialPredicates) {
      stats.trivialPredicatesTime.start();
      abs = handleTrivialPredicates(remainingPredicates, abstractionFormula, pathFormula);
      stats.trivialPredicatesTime.stop();
    }

    // add invariants to abstraction formula if available
    Region invariant = locationBasedInvariantSupplier.getInvariantFor(location);
    if (invariant != null) {
      abs = rmgr.makeAnd(abs, invariant);
      // Calculate the set of predicates we still need to use for abstraction.
      Iterables.removeIf(remainingPredicates, in(amgr.extractPredicates(invariant)));
    }

    try (ProverEnvironment thmProver = solver.newProverEnvironment()) {
      thmProver.push(f);

      if (remainingPredicates.isEmpty() && (abstractionType != AbstractionType.ELIMINATION)) {
        stats.numSatCheckAbstractions++;

        stats.abstractionSolveTime.start();
        boolean feasibility;
        try {
          feasibility = !thmProver.isUnsat();
        } finally {
          stats.abstractionSolveTime.stop();
        }

        if (!feasibility) {
          abs = rmgr.makeFalse();
        }

      } else if (abstractionType == AbstractionType.ELIMINATION) {
        stats.quantifierEliminationTime.start();
        try {
          abs = rmgr.makeAnd(abs,
              eliminateIrrelevantVariablePropositions(f, ssa));
        } finally {
          stats.quantifierEliminationTime.stop();
        }
      } else {
        if (abstractionType != AbstractionType.BOOLEAN) {
          // First do cartesian abstraction if desired
          stats.cartesianAbstractionTime.start();
          try {
            abs =
                rmgr.makeAnd(
                    abs, buildCartesianAbstraction(f, ssa, thmProver, remainingPredicates));
          } finally {
            stats.cartesianAbstractionTime.stop();
          }
        }

        if (abstractionType != AbstractionType.CARTESIAN && !remainingPredicates.isEmpty()) {
          // Last do boolean abstraction if desired and necessary
          stats.numBooleanAbsPredicates += remainingPredicates.size();
          stats.booleanAbstractionTime.start();
          try {
            abs = rmgr.makeAnd(abs, buildBooleanAbstraction(ssa, thmProver, remainingPredicates));
          } finally {
            stats.booleanAbstractionTime.stop();
          }

          // Warning:
          // buildBooleanAbstraction() does not clean up thmProver, so do not use it here.
          // remainingPredicates is now empty.
        }
      }
    }

    AbstractionFormula result = makeAbstractionFormula(abs, ssa, pathFormula);

    if (useCache) {
      abstractionCache.put(absKey, result);

      if (result.isFalse()) {
        unsatisfiabilityCache.add(f);
      }
    }

    long abstractionTime = TimeSpan.sum(stats.abstractionSolveTime.getLengthOfLastInterval(),
        stats.abstractionEnumTime.getLengthOfLastOuterInterval())
        .asMillis();
    logger.log(Level.FINEST, "Computing abstraction took", abstractionTime, "ms");
    logger.log(Level.ALL, "Abstraction result is", result.asFormula());

    if (dumpHardAbstractions && abstractionTime > 10000) {
      // we want to dump "hard" problems...
      Path dumpFile;

      dumpFile = fmgr.formatFormulaOutputFile("abstraction", stats.numCallsAbstraction, "input", 0);
      fmgr.dumpFormulaToFile(f, dumpFile);

      dumpFile =
          fmgr.formatFormulaOutputFile("abstraction", stats.numCallsAbstraction, "predicates", 0);
      try (Writer w = dumpFile.asCharSink(StandardCharsets.UTF_8).openBufferedStream()) {
        Joiner.on('\n').appendTo(w, pPredicates);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Failed to wrote predicates to file");
      }

      dumpFile =
          fmgr.formatFormulaOutputFile("abstraction", stats.numCallsAbstraction, "result", 0);
      fmgr.dumpFormulaToFile(result.asInstantiatedFormula(), dumpFile);
    }

    return result;
  }

  private
  @Nullable
  AbstractionFormula reuseAbstractionIfPossible(
      final AbstractionFormula abstractionFormula,
      final PathFormula pathFormula,
      final BooleanFormula f,
      final CFANode location)
      throws SolverException, InterruptedException {
    stats.abstractionReuseTime.start();
    try (ProverEnvironment reuseEnv = solver.newProverEnvironment()) {
      reuseEnv.push(f);

      Deque<Pair<Integer, Integer>> tryReuseBasedOnPredecessors = new ArrayDeque<>();
      Set<Integer> idsOfStoredAbstractionReused =
          abstractionFormula.getIdsOfStoredAbstractionReused();
      for (Integer id : idsOfStoredAbstractionReused) {
        tryReuseBasedOnPredecessors.add(Pair.of(id, 0));
      }

      if (tryReuseBasedOnPredecessors.isEmpty()) {
        tryReuseBasedOnPredecessors.add(Pair.of(abstractionStorage.getRootAbstractionId(), 0));
      }

      while (!tryReuseBasedOnPredecessors.isEmpty()) {
        final Pair<Integer, Integer> tryBasedOn = tryReuseBasedOnPredecessors.pop();
        final int tryBasedOnAbstractionId = tryBasedOn.getFirst();
        final int tryLevel = tryBasedOn.getSecond();

        if (tryLevel > maxAbstractionReusePrescan) {
          continue;
        }

        Set<AbstractionNode> candidateAbstractions =
            getSuccessorsInAbstractionTree(tryBasedOnAbstractionId);
        Preconditions.checkNotNull(candidateAbstractions);

        //logger.log(Level.WARNING, "Raw candidates based on", tryBasedOnAbstractionId, ":", candidateAbstractions);

        Iterator<AbstractionNode> candidateIterator = candidateAbstractions.iterator();
        while (candidateIterator.hasNext()) {
          AbstractionNode an = candidateIterator.next();
          Preconditions.checkNotNull(an);
          tryReuseBasedOnPredecessors.add(Pair.of(an.getId(), tryLevel + 1));

          if (bfmgr.isTrue(an.getFormula())) {
            candidateIterator.remove();
            continue;
          }

          if (an.getLocationId().isPresent()) {
            if (location.getNodeNumber() != an.getLocationId().get()) {
              candidateIterator.remove();
              continue;
            }
          }
        }

        //logger.log(Level.WARNING, "Filtered candidates", "location", location.getNodeNumber(), "abstraction", tryBasedOnAbstractionId, ":", candidateAbstractions);

        if (candidateAbstractions.size() > 1) {
          logger.log(
              Level.WARNING,
              "Too many abstraction candidates on location",
              location,
              "for abstraction",
              tryBasedOnAbstractionId,
              ". Disabling abstraction reuse!");
          this.abstractionReuseDisabledBecauseOfAmbiguity = true;
          tryReuseBasedOnPredecessors.clear();
          continue;
        }

        Set<Integer> reuseIds = Sets.newTreeSet();
        BooleanFormula reuseFormula = bfmgr.makeBoolean(true);
        for (AbstractionNode an : candidateAbstractions) {
          reuseFormula = bfmgr.and(reuseFormula, an.getFormula());
          abstractionStorage.markAbstractionBeingReused(an.getId());
          reuseIds.add(an.getId());
        }
        BooleanFormula instantiatedReuseFormula =
            fmgr.instantiate(reuseFormula, pathFormula.getSsa());

        stats.abstractionReuseImplicationTime.start();
        reuseEnv.push(bfmgr.not(instantiatedReuseFormula));
        boolean implication = reuseEnv.isUnsat();
        reuseEnv.pop();
        stats.abstractionReuseImplicationTime.stop();

        if (implication) {
          stats.numAbstractionReuses++;

          Region reuseFormulaRegion = amgr.convertFormulaToRegion(reuseFormula);
          return new AbstractionFormula(
              fmgr,
              reuseFormulaRegion,
              reuseFormula,
              instantiatedReuseFormula,
              pathFormula,
              reuseIds);
        }
      }
    } finally {
      stats.abstractionReuseTime.stop();
    }
    return null; //no abstraction could be reused
  }

  private Region eliminateIrrelevantVariablePropositions(BooleanFormula pF, SSAMap pSsa)
      throws InterruptedException, SolverException {

    BooleanFormula eliminationResult = fmgr.uninstantiate(
        fmgr.eliminateDeadVariables(pF, pSsa));

    return amgr.convertFormulaToRegion(eliminationResult);

  }

  /**
   * Extract all relevant predicates (with respect to a given formula)
   * from a given set of predicates.
   *
   * Currently the check is syntactically, i.e.,
   * a predicate is relevant if it refers to at least one variable
   * that also occurs in f.
   *
   * A predicate that is just "false" or "true" is also filtered out.
   *
   * @param pPredicates The set of predicates.
   * @param f           The formula that determines which variables and predicates are relevant.
   * @param ssa         The SSA map to use for instantiating predicates.
   * @param pLocation   the location that should be used
   * @return A subset of pPredicates.
   */
  private Collection<AbstractionPredicate> getRelevantPredicates(
      final Collection<AbstractionPredicate> pPredicates,
      final BooleanFormula f,
      final SSAMap ssa,
      final CFANode pLocation) {

    Set<String> variables = fmgr.extractVariableNames(f);
    // LinkedList keeps order (important to avoid non-determinism) and supports efficient removal.
    Collection<AbstractionPredicate> relevantPredicates = new LinkedList<>();

    for (AbstractionPredicate predicate : pPredicates) {
      final BooleanFormula predicateTerm = predicate.getSymbolicAtom();
      if (bfmgr.isFalse(predicateTerm)) {
        // Ignore predicate "false", it means "check for satisfiability".
        // We do this implicitly.
        logger.log(Level.FINEST, "Ignoring predicate 'false'");
        continue;
      }

      BooleanFormula instantiatedPredicate = fmgr.instantiate(predicateTerm, ssa);
      Set<String> predVariables = fmgr.extractVariableNames(instantiatedPredicate);

      if (predVariables.isEmpty()
          || !Sets.intersection(predVariables, variables).isEmpty()) {
        // Predicates without variables occur (for example, talking about UFs).
        // We do not know whether they are relevant, so we have to add them.
        relevantPredicates.add(predicate);

      } else {
        logger.log(Level.FINEST, "Ignoring predicate about variables", predVariables);
      }
    }

    return relevantPredicates;
  }

  /**
   * This method finds predicates whose truth value after the
   * abstraction computation is trivially known,
   * and returns a region with these predicates,
   * so that these predicates also do not need to be used in the abstraction computation.
   *
   * @param pPredicates   The set of predicates. Each predicate that is handled will be removed from
   *                      the set.
   * @param pOldAbs       An abstraction formula that determines which variables and predicates are
   *                      relevant.
   * @param pBlockFormula A path formula that determines which variables and predicates are
   *                      relevant.
   * @return A region of predicates from pPredicates that is entailed by (pOldAbs & pBlockFormula)
   */
  private Region handleTrivialPredicates(
      final Collection<AbstractionPredicate> pPredicates,
      final AbstractionFormula pOldAbs,
      final PathFormula pBlockFormula)
      throws SolverException, InterruptedException {

    final SSAMap ssa = pBlockFormula.getSsa();
    final Set<String> blockVariables = fmgr.extractVariableNames(pBlockFormula.getFormula());
    final Region oldAbs = pOldAbs.asRegion();

    final RegionCreator regionCreator = amgr.getRegionCreator();
    Region region = regionCreator.makeTrue();

    final Iterator<AbstractionPredicate> predicateIt = pPredicates.iterator();
    while (predicateIt.hasNext()) {
      final AbstractionPredicate predicate = predicateIt.next();
      final BooleanFormula predicateTerm = predicate.getSymbolicAtom();

      BooleanFormula instantiatedPredicate = fmgr.instantiate(predicateTerm, ssa);
      final Set<String> predVariables = fmgr.extractVariableNames(instantiatedPredicate);

      if (Sets.intersection(predVariables, blockVariables).isEmpty()) {
        // predicate irrelevant with respect to block formula

        final Region predicateVar = predicate.getAbstractVariable();
        if (amgr.entails(oldAbs, predicateVar)) {
          // predicate is unconditionally implied by old abs,
          // we can just copy it to the output
          region = regionCreator.makeAnd(region, predicateVar);
          predicateIt.remove(); // mark predicate as handled
          stats.numTrivialPredicates++;
          logger.log(Level.FINEST, "Predicate", predicate,
              "is unconditionally true in old abstraction and can be copied to the result.");

        } else {
          final Region negatedPredicateVar = regionCreator.makeNot(predicateVar);
          if (amgr.entails(oldAbs, negatedPredicateVar)) {
            // negated predicate is unconditionally implied by old abs,
            // we can just copy it to the output
            region = regionCreator.makeAnd(region, negatedPredicateVar);
            predicateIt.remove(); // mark predicate as handled
            stats.numTrivialPredicates++;
            logger.log(Level.FINEST, "Negation of predicate", predicate,
                "is unconditionally true in old abstraction and can be copied to the result.");

          } else {
            // predicate is used in old abs and there is no easy way to handle it
            logger.log(Level.FINEST, "Predicate", predicate,
                "is relevant because it appears in the old abstraction.");
          }
        }
      }
    }

    assert amgr.entails(oldAbs, region);

    return region;
  }

  /**
   * Compute an abstraction of a single boolean formula.
   *
   * @param f            The formula to be abstracted. Needs to be instantiated with the indices
   *                     from <code>blockFormula.getSssa()</code>.
   * @param blockFormula A path formula that is not used for the abstraction, but will be used as
   *                     the block formula in the resulting AbstractionFormula instance.
   * @param predicates   The set of predicates used for abstraction.
   * @return An AbstractionFormula instance representing an abstraction of f with blockFormula as
   * the block formula.
   */
  public AbstractionFormula buildAbstraction(
      final CFANode location,
      final BooleanFormula f,
      final PathFormula blockFormula,
      final Collection<AbstractionPredicate> predicates)
      throws SolverException, InterruptedException {

    PathFormula pf =
        new PathFormula(f, blockFormula.getSsa(), blockFormula.getPointerTargetSet(), 0);

    AbstractionFormula emptyAbstraction = makeTrueAbstractionFormula(null);
    AbstractionFormula newAbstraction =
        buildAbstraction(location, emptyAbstraction, pf, predicates);

    // fix block formula in result
    return new AbstractionFormula(fmgr, newAbstraction.asRegion(),
        newAbstraction.asFormula(), newAbstraction.asInstantiatedFormula(),
        blockFormula, noAbstractionReuse);
  }

  /**
   * Create an abstraction from a single boolean formula without actually
   * doing any abstraction computation. The formula is just converted into a
   * region, but the result is equivalent to the input.
   * This can be used to simply view the formula as a region.
   * If BDDs are used, the result of this method is a minimized form of the input.
   *
   * @param f            The formula to be converted to a region. Must NOT be instantiated!
   * @param blockFormula A path formula that is not used for the abstraction, but will be used as
   *                     the block formula in the resulting AbstractionFormula instance. Also it's
   *                     SSAMap will be used for instantiating the result.
   * @return An AbstractionFormula instance representing f with blockFormula as the block formula.
   */
  public AbstractionFormula buildAbstraction(
      final BooleanFormula f,
      final PathFormula blockFormula) {
    Region r = amgr.convertFormulaToRegion(f);
    return makeAbstractionFormula(r, blockFormula.getSsa(), blockFormula);
  }

  /**
   * Compute a Cartesian abstraction of a formula given a set of predicates.
   * The abstracted formula is expected to have been pushed onto the solver stack already.
   *
   * @param f           The (instantiated) formula to abstract, only used as cache key.
   * @param ssa         The SSAMap for instantiating predicates such that it matches f.
   * @param thmProver   The solver to use with the input formula on the stack.
   * @param pPredicates The set of predicates. Each predicate that is handled will be removed from
   *                    the set.
   * @return A over-approximation of f.
   */
  private Region buildCartesianAbstraction(
      final BooleanFormula f,
      final SSAMap ssa,
      final ProverEnvironment thmProver,
      final Collection<AbstractionPredicate> pPredicates)
      throws SolverException, InterruptedException {

    stats.abstractionSolveTime.start();
    boolean feasibility = !thmProver.isUnsat();
    stats.abstractionSolveTime.stop();

    if (!feasibility) {
      // abstract post leads to false, we can return immediately
      return rmgr.makeFalse();
    }

    if (!warnedOfCartesianAbstraction && !fmgr.isPurelyConjunctive(f)) {
      logger.log(Level.WARNING,
          "Using cartesian abstraction when formulas contain disjunctions may be imprecise. "
              + "This might lead to failing refinements.");
      warnedOfCartesianAbstraction = true;
    }

    stats.abstractionEnumTime.startOuter();
    try {
      Region absbdd = rmgr.makeTrue();

      // check whether each of the predicate is implied in the next state...

      final Iterator<AbstractionPredicate> predicateIt = pPredicates.iterator();
      while (predicateIt.hasNext()) {
        final AbstractionPredicate p = predicateIt.next();
        Pair<BooleanFormula, AbstractionPredicate> cacheKey = Pair.of(f, p);
        if (useCache && cartesianAbstractionCache.containsKey(cacheKey)) {
          byte predVal = cartesianAbstractionCache.get(cacheKey);
          stats.numCartesianAbsPredicatesCached++;

          stats.abstractionEnumTime.getCurentInnerTimer().start();
          Region v = p.getAbstractVariable();
          if (predVal == -1) { // pred is false
            stats.numCartesianAbsPredicates++;
            v = rmgr.makeNot(v);
            absbdd = rmgr.makeAnd(absbdd, v);
          } else if (predVal == 1) { // pred is true
            stats.numCartesianAbsPredicates++;
            absbdd = rmgr.makeAnd(absbdd, v);
          } else {
            assert predVal == 0 : "predicate value is neither false, true, nor unknown";
          }
          stats.abstractionEnumTime.getCurentInnerTimer().stop();

        } else {
          logger.log(Level.ALL, "DEBUG_1",
              "CHECKING VALUE OF PREDICATE: ", p.getSymbolicAtom());

          // instantiate the definition of the predicate
          BooleanFormula predTrue = fmgr.instantiate(p.getSymbolicAtom(), ssa);
          BooleanFormula predFalse = bfmgr.not(predTrue);

          // check whether this predicate has a truth value in the next
          // state
          byte predVal = 0; // pred is neither true nor false

          thmProver.push(predFalse);
          boolean isTrue = thmProver.isUnsat();
          thmProver.pop();

          if (isTrue) {
            stats.numCartesianAbsPredicates++;
            stats.abstractionEnumTime.getCurentInnerTimer().start();
            Region v = p.getAbstractVariable();
            absbdd = rmgr.makeAnd(absbdd, v);
            predicateIt.remove(); // mark predicate as handled
            stats.abstractionEnumTime.getCurentInnerTimer().stop();

            predVal = 1;
          } else {
            // check whether it's false...
            thmProver.push(predTrue);
            boolean isFalse = thmProver.isUnsat();
            thmProver.pop();

            if (isFalse) {
              stats.numCartesianAbsPredicates++;
              stats.abstractionEnumTime.getCurentInnerTimer().start();
              Region v = p.getAbstractVariable();
              v = rmgr.makeNot(v);
              absbdd = rmgr.makeAnd(absbdd, v);
              predicateIt.remove(); // mark predicate as handled
              stats.abstractionEnumTime.getCurentInnerTimer().stop();

              predVal = -1;
            }
          }

          if (useCache) {
            cartesianAbstractionCache.put(cacheKey, predVal);
          }
        }
      }

      return absbdd;

    } finally {
      stats.abstractionEnumTime.stopOuter();
    }
  }

  private BooleanFormula buildFormula(BooleanFormula symbFormula) {

    if (fmgr.useBitwiseAxioms()) {
      BooleanFormula bitwiseAxioms = fmgr.getBitwiseAxioms(symbFormula);
      if (!bfmgr.isTrue(bitwiseAxioms)) {
        symbFormula = bfmgr.and(symbFormula, bitwiseAxioms);

        logger.log(Level.ALL, "DEBUG_3", "ADDED BITWISE AXIOMS:", bitwiseAxioms);
      }
    }

    return symbFormula;
  }

  /**
   * Compute a Boolean abstraction of a formula given a set of predicates.
   * The abstracted formula is expected to have been pushed onto the solver stack already.
   *
   * @param ssa        The SSAMap for instantiating predicates such that it matches f.
   * @param thmProver  The solver to use with the input formula on the stack.
   * @param predicates The set of predicates. Each predicate that is handled will be removed from
   *                   the set (and Boolean abstraction handles all predicates so the set is empty
   *                   afterwards!).
   * @return A over-approximation of f.
   */
  private Region buildBooleanAbstraction(
      final SSAMap ssa,
      final ProverEnvironment thmProver,
      final Collection<AbstractionPredicate> predicates)
      throws InterruptedException, SolverException {

    // build the definition of the predicates, and instantiate them
    // also collect all predicate variables so that the solver knows for which
    // variables we want to have the satisfying assignments
    BooleanFormula predDef = bfmgr.makeBoolean(true);
    List<BooleanFormula> predVars = new ArrayList<>(predicates.size());

    for (AbstractionPredicate p : predicates) {
      // get propositional variable and definition of predicate
      BooleanFormula var = p.getSymbolicVariable();
      BooleanFormula def = p.getSymbolicAtom();
      assert !bfmgr.isFalse(def);
      def = fmgr.instantiate(def, ssa);

      // build the formula (var <-> def) and add it to the list of definitions
      BooleanFormula equiv = bfmgr.equivalence(var, def);
      predDef = bfmgr.and(predDef, equiv);

      predVars.add(var);
    }

    // the formula is (abstractionFormula & pathFormula & predDef)
    thmProver.push(predDef);
    AllSatCallbackImpl callback = new AllSatCallbackImpl();
    Region result = thmProver.allSat(callback, predVars);

    // pop() is actually costly sometimes, and we delete the environment anyway
    // thmProver.pop();

    // update statistics
    int numModels = callback.getCount();
    if (numModels < Integer.MAX_VALUE) {
      stats.maxAllSatCount = Math.max(numModels, stats.maxAllSatCount);
      stats.allSatCount += numModels;
    }

    // Not strictly necessary, but mark all predicates as handled
    predicates.clear();

    return result;
  }

  private class AllSatCallbackImpl implements AllSatCallback<Region> {

    private final RegionBuilder builder;

    private Timer regionTime = null;

    private int count = 0;

    private Region formula;

    private AllSatCallbackImpl() {
      builder = rmgr.builder(shutdownNotifier);

      stats.abstractionSolveTime.start();
    }

    @Override
    public void apply(List<BooleanFormula> model) {
      if (count == 0) {
        stats.abstractionSolveTime.stop();
        stats.abstractionEnumTime.startOuter();
        regionTime = stats.abstractionEnumTime.getCurentInnerTimer();
      }

      regionTime.start();

      // the abstraction is created simply by taking the disjunction
      // of all the models found by the all-sat-loop, and storing them in a BDD
      // first, let's create the BDD corresponding to the model
      builder.startNewConjunction();
      for (BooleanFormula f : model) {
        Optional<BooleanFormula> inner = fmgr.stripNegation(f);
        Region region = amgr.getPredicate(inner.or(f)).getAbstractVariable();
        if (inner.isPresent()) {
          // TODO: possible bug if the predicate itself contains the negation.
          builder.addNegativeRegion(region);
        } else {
          builder.addPositiveRegion(region);
        }
      }
      builder.finishConjunction();

      count++;

      regionTime.stop();

    }

    @Override
    public Region getResult() throws InterruptedException {
      if (stats.abstractionSolveTime.isRunning()) {
        stats.abstractionSolveTime.stop();
      } else {
        stats.abstractionEnumTime.stopOuter();
      }

      if (formula == null) {
        stats.abstractionEnumTime.startBoth();
        try {
          formula = builder.getResult();
          builder.close();
        } finally {
          stats.abstractionEnumTime.stopBoth();
        }
      }
      return formula;
    }

    private int getCount() {
      return count;
    }
  }

  /**
   * Checks if a1 => a2
   */
  public boolean checkCoverage(AbstractionFormula a1, AbstractionFormula a2)
      throws SolverException, InterruptedException {
    return amgr.entails(a1.asRegion(), a2.asRegion());
  }

  /**
   * Checks if (a1 & p1) => a2
   */
  public boolean checkCoverage(AbstractionFormula a1, PathFormula p1, AbstractionFormula a2)
      throws SolverException, InterruptedException {
    BooleanFormula absFormula = a1.asInstantiatedFormula();
    BooleanFormula symbFormula = buildFormula(p1.getFormula());
    BooleanFormula a = bfmgr.and(absFormula, symbFormula);

    // get formula of a2 with the indices of p1
    BooleanFormula b = fmgr.instantiate(a2.asFormula(), p1.getSsa());

    return solver.implies(a, b);
  }

  /**
   * Checks if an abstraction formula and a pathFormula are unsatisfiable.
   *
   * @param abstractionFormula the abstraction formula
   * @param pathFormula        the path formula
   * @return unsat(pAbstractionFormula & pPathFormula)
   */
  public boolean unsat(AbstractionFormula abstractionFormula, PathFormula pathFormula)
      throws SolverException, InterruptedException {

    BooleanFormula absFormula = abstractionFormula.asInstantiatedFormula();
    BooleanFormula symbFormula = buildFormula(pathFormula.getFormula());
    BooleanFormula f = bfmgr.and(absFormula, symbFormula);

    logger.log(Level.ALL, "Checking satisfiability of formula", f);

    return solver.isUnsat(f);
  }

  public AbstractionFormula makeTrueAbstractionFormula(PathFormula pPreviousBlockFormula) {
    if (pPreviousBlockFormula == null) {
      pPreviousBlockFormula = pfmgr.makeEmptyPathFormula();
    }

    return new AbstractionFormula(fmgr, amgr.getRegionCreator().makeTrue(), bfmgr.makeBoolean(true),
        bfmgr.makeBoolean(true),
        pPreviousBlockFormula, noAbstractionReuse);
  }

  /**
   * Conjuncts two abstractions.
   * Both need to have the same block formula.
   */
  public AbstractionFormula makeAnd(AbstractionFormula a1, AbstractionFormula a2) {
    checkArgument(a1.getBlockFormula().equals(a2.getBlockFormula()));

    Region region = amgr.getRegionCreator().makeAnd(a1.asRegion(), a2.asRegion());
    BooleanFormula formula = fmgr.makeAnd(a1.asFormula(), a2.asFormula());
    BooleanFormula instantiatedFormula =
        fmgr.makeAnd(a1.asInstantiatedFormula(), a2.asInstantiatedFormula());

    return new AbstractionFormula(fmgr, region, formula, instantiatedFormula, a1.getBlockFormula(),
        noAbstractionReuse);
  }

  private AbstractionFormula makeAbstractionFormula(
      Region abs,
      SSAMap ssaMap,
      PathFormula blockFormula) {
    BooleanFormula symbolicAbs = amgr.convertRegionToFormula(abs);
    BooleanFormula instantiatedSymbolicAbs = fmgr.instantiate(symbolicAbs, ssaMap);

    if (simplifyAbstractionFormula) {
      symbolicAbs = fmgr.simplify(symbolicAbs);
      instantiatedSymbolicAbs = fmgr.simplify(instantiatedSymbolicAbs);
    }

    return new AbstractionFormula(fmgr, abs, symbolicAbs, instantiatedSymbolicAbs, blockFormula,
        noAbstractionReuse);
  }

  /**
   * Remove a set of predicates from an abstraction.
   *
   * @param oldAbstraction   The abstraction to start from.
   * @param removePredicates The predicate to remove.
   * @param ssaMap           The SSAMap to use for instantiating the new abstraction.
   * @return A new abstraction similar to the old one without the predicates.
   */
  public AbstractionFormula reduce(
      AbstractionFormula oldAbstraction,
      Collection<AbstractionPredicate> removePredicates, SSAMap ssaMap) {
    RegionCreator rmgr = amgr.getRegionCreator();

    Region newRegion = oldAbstraction.asRegion();
    for (AbstractionPredicate predicate : removePredicates) {
      newRegion = rmgr.makeExists(newRegion, predicate.getAbstractVariable());
    }

    return makeAbstractionFormula(newRegion, ssaMap, oldAbstraction.getBlockFormula());
  }

  /**
   * Extend an abstraction by a set of predicates.
   *
   * @param reducedAbstraction The abstraction to extend.
   * @param sourceAbstraction  The abstraction where to take the predicates from.
   * @param relevantPredicates The predicates to add.
   * @param newSSA             The SSAMap to use for instantiating the new abstraction.
   * @param blockFormula       block formula of reduced abstraction state
   * @return A new abstraction similar to the old one with some more predicates.
   */
  public AbstractionFormula expand(
      Region reducedAbstraction,
      Region sourceAbstraction,
      Collection<AbstractionPredicate> relevantPredicates,
      SSAMap newSSA,
      PathFormula blockFormula) {
    RegionCreator rmgr = amgr.getRegionCreator();

    for (AbstractionPredicate predicate : relevantPredicates) {
      sourceAbstraction = rmgr.makeExists(sourceAbstraction,
          predicate.getAbstractVariable());
    }

    Region expandedRegion = rmgr.makeAnd(reducedAbstraction, sourceAbstraction);

    return makeAbstractionFormula(expandedRegion, newSSA, blockFormula);
  }

  /**
   * Extract all atoms from a formula and create predicates for them.
   * If instead a single predicate should be created for the whole formula,
   * call {@link #getPredicateFor(BooleanFormula)} instead.
   *
   * @param pFormula The formula with the atoms (with SSA indices).
   * @return A (possibly empty) collection of AbstractionPredicates without duplicates.
   */
  public Collection<AbstractionPredicate> getPredicatesForAtomsOf(BooleanFormula pFormula) {
    if (bfmgr.isFalse(pFormula)) {
      return ImmutableList.of(amgr.makeFalsePredicate());
    }

    Set<BooleanFormula> atoms = fmgr.extractAtoms(pFormula, splitItpAtoms);

    List<AbstractionPredicate> preds = new ArrayList<>(atoms.size());

    for (BooleanFormula atom : atoms) {
      preds.add(amgr.makePredicate(fmgr.uninstantiate(atom)));
    }

    amgr.reorderPredicates();
    return preds;
  }

  /**
   * Create a single AbstractionPredicate representing a formula.
   * If instead a predicate should be used for each atom in this formula,
   * call {@link #getPredicatesForAtomsOf(BooleanFormula)}.
   *
   * @param pFormula The formula to use (with SSA indices), may not simply be "true".
   * @return A single abstraction predicate.
   */
  public AbstractionPredicate getPredicateFor(BooleanFormula pFormula) {
    checkArgument(!bfmgr.isTrue(pFormula));

    return amgr.makePredicate(fmgr.uninstantiate(pFormula));
  }

  // delegate methods

  public AbstractionPredicate makeFalsePredicate() {
    return amgr.makeFalsePredicate();
  }

  /**
   * Return the set of predicates that occur in a region.
   *
   * Note: this method currently fails with SymbolicRegionManager,
   * and it probably cannot really be fixed either, because when using symbolic regions
   * we do not know what are the predicates (a predicate does not need to be an SMT atom,
   * it can be larger).
   *
   * Thus better avoid using this method if possible.
   */
  public Set<AbstractionPredicate> extractPredicates(Region pRegion) {
    return amgr.extractPredicates(pRegion);
  }

  private Set<AbstractionNode> getSuccessorsInAbstractionTree(int pIdOfLastAbstractionReused) {
    Preconditions.checkNotNull(reuseAbstractionsFrom);
    return abstractionStorage.getSuccessorAbstractions(pIdOfLastAbstractionReused);
  }

}
