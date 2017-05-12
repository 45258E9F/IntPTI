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
package org.sosy_lab.cpachecker.core.algorithm.invariants;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.LazyFutureTask;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier.TrivialInvariantSupplier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.Or;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Class that encapsulates invariant generation by using the CPAAlgorithm
 * with an appropriate configuration.
 * Supports synchronous and asynchronous execution,
 * and continuously-refined invariants.
 */
@Options(prefix = "invariantGeneration")
public class CPAInvariantGenerator extends AbstractInvariantGenerator
    implements StatisticsProvider {

  private static class CPAInvariantGeneratorStatistics implements Statistics {

    final Timer invariantGeneration = new Timer();

    @Override
    public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
      out.println("Time for invariant generation:   " + invariantGeneration);
    }

    @Override
    public String getName() {
      return "CPA-based invariant generator";
    }
  }

  @Options(prefix = "invariantGeneration")
  private static class InvariantGeneratorOptions {


    @Option(secure = true, description = "generate invariants in parallel to the normal analysis")
    private boolean async = false;

    @Option(secure = true, description = "adjust invariant generation conditions if supported by the analysis")
    private boolean adjustConditions = false;

  }

  @Option(secure = true, name = "config",
      required = true,
      description = "configuration file for invariant generation")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path configFile;

  private final CPAInvariantGeneratorStatistics stats = new CPAInvariantGeneratorStatistics();
  private final LogManager logger;
  private final CPAAlgorithm algorithm;
  private final ConfigurableProgramAnalysis cpa;
  private final ReachedSetFactory reachedSetFactory;
  private final CFA cfa;

  private final ShutdownManager shutdownManager;

  private final int iteration;

  // After start(), this will hold a Future for the final result of the invariant generation.
  // We use a Future instead of just the atomic reference below
  // to be able to ask for termination and see thrown exceptions.
  private Future<FormulaAndTreeSupplier> invariantGenerationFuture = null;

  private volatile boolean programIsSafe = false;

  private final ShutdownRequestListener shutdownListener = new ShutdownRequestListener() {

    @Override
    public void shutdownRequested(String pReason) {
      if (!invariantGenerationFuture.isDone() && !programIsSafe) {
        invariantGenerationFuture.cancel(true);
      }
    }
  };

  private Optional<ShutdownManager> shutdownOnSafeNotifier;

  /**
   * Creates a new {@link CPAInvariantGenerator}.
   *
   * @param pConfig                the configuration options.
   * @param pLogger                the logger to be used.
   * @param pShutdownManager       shutdown notifier to shutdown the invariant generator.
   * @param pShutdownOnSafeManager optional shutdown notifier that will be notified if the invariant
   *                               generator proves safety.
   * @param pCFA                   the CFA to run the CPA on.
   * @return a new {@link CPAInvariantGenerator}.
   * @throws InvalidConfigurationException if the configuration is invalid.
   * @throws CPAException                  if the CPA cannot be created.
   */
  public static InvariantGenerator create(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownManager pShutdownManager,
      final Optional<ShutdownManager> pShutdownOnSafeManager,
      final CFA pCFA)
      throws InvalidConfigurationException, CPAException {
    return create(pConfig, pLogger, pShutdownManager, pShutdownOnSafeManager, pCFA,
        Collections.<Automaton>emptyList());
  }

  /**
   * Creates a new {@link CPAInvariantGenerator}.
   *
   * @param pConfig                the configuration options.
   * @param pLogger                the logger to be used.
   * @param pShutdownManager       shutdown notifier to shutdown the invariant generator.
   * @param pShutdownOnSafeManager optional shutdown notifier that will be notified if the invariant
   *                               generator proves safety.
   * @param pCFA                   the CFA to run the CPA on.
   * @param additionalAutomata     additional specification automata that should be used during
   *                               invariant generation
   * @return a new {@link CPAInvariantGenerator}.
   * @throws InvalidConfigurationException if the configuration is invalid.
   * @throws CPAException                  if the CPA cannot be created.
   */
  public static InvariantGenerator create(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownManager pShutdownManager,
      final Optional<ShutdownManager> pShutdownOnSafeManager,
      final CFA pCFA,
      final List<Automaton> additionalAutomata)
      throws InvalidConfigurationException, CPAException {

    InvariantGeneratorOptions options = new InvariantGeneratorOptions();
    pConfig.inject(options);
    final ShutdownManager childShutdownManager =
        ShutdownManager.createWithParent(pShutdownManager.getNotifier());

    CPAInvariantGenerator cpaInvariantGenerator =
        new CPAInvariantGenerator(
            pConfig,
            pLogger.withComponentName("CPAInvariantGenerator"),
            childShutdownManager,
            pShutdownOnSafeManager,
            1,
            pCFA,
            additionalAutomata);

    InvariantGenerator invariantGenerator = cpaInvariantGenerator;
    final Function<CPAInvariantGenerator, CPAInvariantGenerator> adjust;
    if (options.adjustConditions) {
      adjust =
          new Function<CPAInvariantGenerator, CPAInvariantGenerator>() {

            @Override
            public CPAInvariantGenerator apply(CPAInvariantGenerator pToAdjust) {
              ConfigurableProgramAnalysis cpa = pToAdjust.cpa;
              LogManager logger = pToAdjust.logger;
              List<AdjustableConditionCPA> conditionCPAs =
                  CPAs.asIterable(cpa).filter(AdjustableConditionCPA.class).toList();
              CPAInvariantGenerator result = pToAdjust;
              try {
                if (adjustConditions(logger, conditionCPAs)) {
                  result =
                      new CPAInvariantGenerator(
                          pConfig,
                          pLogger,
                          childShutdownManager,
                          pShutdownOnSafeManager,
                          pToAdjust.iteration + 1,
                          pCFA,
                          pToAdjust.reachedSetFactory,
                          cpa,
                          pToAdjust.algorithm);
                }
              } catch (InvalidConfigurationException e) {
                pLogger.logUserException(
                    Level.WARNING, e, "Creating adjusted invariant generator failed");
              } finally {
                if (result == pToAdjust) {
                  CPAs.closeCpaIfPossible(pToAdjust.cpa, pToAdjust.logger);
                  CPAs.closeIfPossible(pToAdjust.algorithm, pToAdjust.logger);
                }
              }
              return result;
            }

            private boolean adjustConditions(
                LogManager pLogger, List<AdjustableConditionCPA> pConditionCPAs) {

              boolean adjusted = false;

              // Adjust precision if at least one CPA can do it.
              for (AdjustableConditionCPA cpa : pConditionCPAs) {
                if (cpa.adjustPrecision()) {
                  pLogger.log(Level.INFO, "Adjusting precision for CPA", cpa);
                  adjusted = true;
                }
              }
              if (!adjusted) {
                pLogger.log(
                    Level.INFO,
                    "None of the CPAs could adjust precision, " + "stopping invariant generation");
              }
              return adjusted;
            }
          };
    } else {
      adjust = new Function<CPAInvariantGenerator, CPAInvariantGenerator>() {

        @Override
        public CPAInvariantGenerator apply(CPAInvariantGenerator pArg0) {
          CPAs.closeCpaIfPossible(pArg0.cpa, pArg0.logger);
          CPAs.closeIfPossible(pArg0.algorithm, pArg0.logger);
          return pArg0;
        }

      };
    }
    invariantGenerator =
        new AdjustableInvariantGenerator<>(
            pShutdownManager.getNotifier(), cpaInvariantGenerator, adjust);
    if (options.async) {
      invariantGenerator =
          new AutoAdjustingInvariantGenerator<>(
              pShutdownManager.getNotifier(), cpaInvariantGenerator, adjust);
    }
    return invariantGenerator;
  }

  private CPAInvariantGenerator(
      final Configuration config,
      final LogManager pLogger,
      final ShutdownManager pShutdownManager,
      Optional<ShutdownManager> pShutdownOnSafeManager,
      final int pIteration,
      final CFA pCFA, List<Automaton> pAdditionalAutomata)
      throws InvalidConfigurationException, CPAException {
    config.inject(this);
    logger = pLogger;
    shutdownManager = pShutdownManager;
    shutdownOnSafeNotifier = pShutdownOnSafeManager;
    iteration = pIteration;

    Configuration invariantConfig;
    try {
      ConfigurationBuilder configBuilder =
          Configuration.builder().copyOptionFrom(config, "specification");

      configBuilder.loadFromFile(configFile);
      invariantConfig = configBuilder.build();
    } catch (IOException e) {
      throw new InvalidConfigurationException(
          "could not read configuration file for invariant generation: " + e.getMessage(), e);
    }

    reachedSetFactory = new ReachedSetFactory(invariantConfig);
    cfa = pCFA;
    cpa =
        new CPABuilder(invariantConfig, logger, shutdownManager.getNotifier(), reachedSetFactory)
            .buildsCPAWithWitnessAutomataAndSpecification(cfa, pAdditionalAutomata);
    algorithm = CPAAlgorithm.create(cpa, logger, invariantConfig, shutdownManager.getNotifier());
  }

  private CPAInvariantGenerator(
      final Configuration config,
      final LogManager pLogger,
      final ShutdownManager pShutdownManager,
      Optional<ShutdownManager> pShutdownOnSafeManager,
      final int pIteration,
      final CFA pCFA,
      ReachedSetFactory pReachedSetFactory,
      ConfigurableProgramAnalysis pCPA,
      CPAAlgorithm pAlgorithm) throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
    shutdownManager = pShutdownManager;
    shutdownOnSafeNotifier = pShutdownOnSafeManager;
    iteration = pIteration;

    reachedSetFactory = pReachedSetFactory;
    cfa = pCFA;
    cpa = pCPA;
    algorithm = pAlgorithm;
  }

  @Override
  public void start(final CFANode initialLocation) {
    checkState(invariantGenerationFuture == null);

    Callable<FormulaAndTreeSupplier> task = new InvariantGenerationTask(initialLocation);
    // create future for lazy synchronous invariant generation
    invariantGenerationFuture = new LazyFutureTask<>(task);

    shutdownManager.getNotifier().registerAndCheckImmediately(shutdownListener);
  }

  @Override
  public void cancel() {
    checkState(invariantGenerationFuture != null);
    shutdownManager.requestShutdown("Invariant generation cancel requested.");
  }

  @Override
  public InvariantSupplier get() throws CPAException, InterruptedException {
    checkState(invariantGenerationFuture != null);

    try {
      return invariantGenerationFuture.get();
    } catch (ExecutionException e) {
      Throwables.propagateIfPossible(e.getCause(), CPAException.class, InterruptedException.class);
      throw new UnexpectedCheckedException("invariant generation", e.getCause());
    } catch (CancellationException e) {
      shutdownManager.getNotifier().shutdownIfNecessary();
      throw e;
    }
  }

  @Override
  public ExpressionTreeSupplier getAsExpressionTree() throws CPAException, InterruptedException {
    checkState(invariantGenerationFuture != null);

    try {
      return invariantGenerationFuture.get();
    } catch (ExecutionException e) {
      Throwables.propagateIfPossible(e.getCause(), CPAException.class, InterruptedException.class);
      throw new UnexpectedCheckedException("invariant generation", e.getCause());
    } catch (CancellationException e) {
      shutdownManager.getNotifier().shutdownIfNecessary();
      throw e;
    }
  }

  @Override
  public boolean isProgramSafe() {
    return programIsSafe;
  }

  @Override
  public void injectInvariant(CFANode pLocation, AssumeEdge pAssumption)
      throws UnrecognizedCodeException {
    InvariantsCPA invariantCPA = CPAs.retrieveCPA(cpa, InvariantsCPA.class);
    if (invariantCPA != null) {
      invariantCPA.injectInvariant(pLocation, pAssumption);
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (cpa instanceof StatisticsProvider) {
      ((StatisticsProvider) cpa).collectStatistics(pStatsCollection);
    }
    algorithm.collectStatistics(pStatsCollection);
    pStatsCollection.add(stats);
  }

  private static class LazyLocationMapping {

    private final UnmodifiableReachedSet reachedSet;

    private final AtomicReference<Multimap<CFANode, AbstractState>> statesByLocationRef =
        new AtomicReference<>();

    public LazyLocationMapping(UnmodifiableReachedSet pReachedSet) {
      this.reachedSet = Objects.requireNonNull(pReachedSet);
    }

    public Iterable<AbstractState> get(CFANode pLocation) {
      if (reachedSet instanceof LocationMappedReachedSet) {
        return AbstractStates.filterLocation(reachedSet, pLocation);
      }
      if (statesByLocationRef.get() == null) {
        Multimap<CFANode, AbstractState> statesByLocation = HashMultimap.create();
        for (AbstractState state : reachedSet) {
          for (CFANode location : AbstractStates.extractLocations(state)) {
            statesByLocation.put(location, state);
          }
        }
        this.statesByLocationRef.set(statesByLocation);
        return statesByLocation.get(pLocation);
      }
      return statesByLocationRef.get().get(pLocation);
    }

  }

  /**
   * {@link InvariantSupplier} that extracts invariants from a {@link ReachedSet}
   * with {@link FormulaReportingState}s.
   */
  private static class ReachedSetBasedInvariantSupplier implements InvariantSupplier {

    private final LazyLocationMapping lazyLocationMapping;
    private final LogManager logger;

    private ReachedSetBasedInvariantSupplier(
        LazyLocationMapping pLazyLocationMapping,
        LogManager pLogger) {
      logger = Objects.requireNonNull(pLogger);
      lazyLocationMapping = Objects.requireNonNull(pLazyLocationMapping);
    }

    @Override
    public BooleanFormula getInvariantFor(
        CFANode pLocation,
        FormulaManagerView fmgr,
        PathFormulaManager pfmgr,
        PathFormula pContext) {
      BooleanFormulaManager bfmgr = fmgr.getBooleanFormulaManager();
      BooleanFormula invariant = bfmgr.makeBoolean(false);

      for (AbstractState locState : lazyLocationMapping.get(pLocation)) {
        BooleanFormula f = AbstractStates.extractReportedFormulas(fmgr, locState, pfmgr);
        logger.log(Level.ALL, "Invariant for", pLocation + ":", f);

        invariant = bfmgr.or(invariant, f);
      }
      return invariant;
    }
  }

  private static class ReachedSetBasedExpressionTreeSupplier implements ExpressionTreeSupplier {

    private final LazyLocationMapping lazyLocationMapping;
    private final CFA cfa;

    private ReachedSetBasedExpressionTreeSupplier(
        LazyLocationMapping pLazyLocationMapping,
        CFA pCFA) {
      lazyLocationMapping = Objects.requireNonNull(pLazyLocationMapping);
      cfa = Objects.requireNonNull(pCFA);
    }

    @Override
    public ExpressionTree<Object> getInvariantFor(CFANode pLocation) {
      ExpressionTree<Object> locationInvariant = ExpressionTrees.getFalse();

      Set<InvariantsState> invStates = Sets.newHashSet();
      boolean otherReportingStates = false;

      for (AbstractState locState : lazyLocationMapping.get(pLocation)) {
        ExpressionTree<Object> stateInvariant = ExpressionTrees.getTrue();

        for (ExpressionTreeReportingState expressionTreeReportingState :
            AbstractStates.asIterable(locState).filter(ExpressionTreeReportingState.class)) {
          if (expressionTreeReportingState instanceof InvariantsState) {
            InvariantsState invState = (InvariantsState) expressionTreeReportingState;
            boolean skip = false;
            for (InvariantsState other : invStates) {
              if (invState.isLessOrEqual(other)) {
                skip = true;
                break;
              }
            }
            if (skip) {
              stateInvariant = ExpressionTrees.getFalse();
              continue;
            }
            invStates.add(invState);
          } else {
            otherReportingStates = true;
          }
          stateInvariant =
              And.of(
                  stateInvariant,
                  expressionTreeReportingState.getFormulaApproximation(
                      cfa.getFunctionHead(pLocation.getFunctionName()), pLocation));
        }

        locationInvariant = Or.of(locationInvariant, stateInvariant);
      }

      if (!otherReportingStates && invStates.size() > 1) {
        Set<InvariantsState> newInvStates = Sets.newHashSet();
        for (InvariantsState a : invStates) {
          boolean skip = false;
          for (InvariantsState b : invStates) {
            if (a != b && a.isLessOrEqual(b)) {
              skip = true;
              break;
            }
          }
          if (!skip) {
            newInvStates.add(a);
          }
        }
        if (newInvStates.size() < invStates.size()) {
          locationInvariant = ExpressionTrees.getFalse();
          for (InvariantsState state : newInvStates) {
            locationInvariant =
                Or.of(
                    locationInvariant,
                    state.getFormulaApproximation(
                        cfa.getFunctionHead(pLocation.getFunctionName()), pLocation));
          }
        }
      }

      return locationInvariant;
    }
  }

  /**
   * Callable for creating invariants by running the CPAAlgorithm,
   * potentially in a loop with increasing precision.
   * Returns the final invariants.
   */
  private class InvariantGenerationTask implements Callable<FormulaAndTreeSupplier> {

    private static final String SAFE_MESSAGE =
        "Invariant generation with abstract interpretation proved specification to hold.";
    private final CFANode initialLocation;

    private InvariantGenerationTask(final CFANode pInitialLocation) {
      initialLocation = checkNotNull(pInitialLocation);
    }

    @Override
    public FormulaAndTreeSupplier call() throws Exception {
      stats.invariantGeneration.start();
      try {

        shutdownManager.getNotifier().shutdownIfNecessary();
        logger.log(Level.INFO, "Starting iteration", iteration,
            "of invariant generation with abstract interpretation.");

        return runInvariantGeneration(initialLocation);

      } finally {
        stats.invariantGeneration.stop();
      }
    }

    private FormulaAndTreeSupplier runInvariantGeneration(CFANode pInitialLocation)
        throws CPAException, InterruptedException {

      ReachedSet taskReached = reachedSetFactory.create();
      taskReached
          .add(cpa.getInitialState(pInitialLocation, StateSpacePartition.getDefaultPartition()),
              cpa.getInitialPrecision(pInitialLocation, StateSpacePartition.getDefaultPartition()));

      while (taskReached.hasWaitingState()) {
        if (!algorithm.run(taskReached).isSound()) {
          // ignore unsound invariant and abort
          return new FormulaAndTreeSupplier(
              TrivialInvariantSupplier.INSTANCE,
              org.sosy_lab.cpachecker.core.algorithm.invariants.ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE);
        }
      }

      if (!from(taskReached).anyMatch(IS_TARGET_STATE)) {
        // program is safe (waitlist is empty, algorithm was sound, no target states present)
        logger.log(Level.INFO, SAFE_MESSAGE);
        programIsSafe = true;
        if (shutdownOnSafeNotifier.isPresent()) {
          shutdownOnSafeNotifier.get().requestShutdown(SAFE_MESSAGE);
        }
      }

      checkState(!taskReached.hasWaitingState());
      checkState(!taskReached.isEmpty());
      LazyLocationMapping lazyLocationMapping = new LazyLocationMapping(taskReached);
      return new FormulaAndTreeSupplier(
          new ReachedSetBasedInvariantSupplier(lazyLocationMapping, logger),
          new ReachedSetBasedExpressionTreeSupplier(lazyLocationMapping, cfa));
    }
  }
}