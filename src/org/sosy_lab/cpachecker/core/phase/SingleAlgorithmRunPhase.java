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
package org.sosy_lab.cpachecker.core.phase;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Sets;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.CPAchecker.InitialStatesFor;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory.SpecAutomatonCompositionType;
import org.sosy_lab.cpachecker.core.MainStatistics;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.ExternalCBMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.impact.ImpactAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.phase.result.AlgorithmPhaseResult;
import org.sosy_lab.cpachecker.core.phase.result.CPAPhaseStatus;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProviderImpl;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.util.HashSet;
import java.util.Set;


/**
 * Algorithm phase is one of the most frequently used phase.
 * In this phase, we create a new algorithm and initial reached set with respect of configuration
 *
 * There are two levels of configurations: one is for this phase and another one is for the
 * wrapped algorithm. Configurations for this phase comprise i) whether it should stop after the
 * first error; ii) how initial states are specified; iii) whether we use external CBMC tool
 * rather than CPA; iv) configuration file for wrapped algorithm.
 */
@Options(prefix = "phase.singleAlgorithm")
public class SingleAlgorithmRunPhase extends CPAPhase {

  private Algorithm algorithm;
  private ReachedSet reached;

  private ShutdownManager subShutdownManager;
  private ConfigurableProgramAnalysis subCpa;
  private final String programDenotation;

  // NOTE: the following configurations are upon the algorithm level, thus we should carefully
  // specify them in the phase of algorithm run

  @Option(secure = true, name = "stopAfterError", description = "stop after the first error has "
      + "been found")
  private boolean stopAfterError = true;

  @Option(secure = true, name = "initialStatesFor", description = "start point of analysis")
  private Set<InitialStatesFor> initialStatesFor = Sets.newHashSet(InitialStatesFor.ENTRY);

  @Option(secure = true, description =
      "Partition the initial states based on the containing function "
          + "of the initial location. This option suppresses partitionInitialStates")
  private boolean functionWisePartition = false;

  @Option(secure = true, description = "partition initial states based on the type of location")
  private boolean partitionInitialStates = false;

  @Option(secure = true, name = "useCBMC", description = "use CBMC as external verifier")
  private boolean runCBMCTool = false;

  @Option(secure = true, name = "analysis", description = "the configuration of main analysis")
  @FileOption(Type.REQUIRED_INPUT_FILE)
  private Path mainAnalyisisConfigFile = Paths.get("config/valueAnalysis-symbolic.properties");

  public SingleAlgorithmRunPhase(
      String pIdentifier,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownManager pShutdownManager,
      ShutdownNotifier pShutdownNotifier,
      MainStatistics pStats)
      throws InvalidConfigurationException {
    super(pIdentifier, pConfig, pLogger, pShutdownManager, pShutdownNotifier, pStats);
    config.inject(this);
    // this should be lazily initialized
    subShutdownManager = null;
    subCpa = null;
    algorithm = null;
    reached = null;
    // load program denotation from IO manager
    programDenotation = GlobalInfo.getInstance().getIoManager().getProgramNames();
  }

  @Override
  protected CPAPhaseStatus prevAction() throws Exception {

    CFAInfo cfaInfo = GlobalInfo.getInstance().getCFAInfo().orNull();
    if (cfaInfo == null) {
      throw new InvalidConfigurationException("Invalid CFA set-up");
    }
    CFA cfa = cfaInfo.getCFA();

    ConfigurationBuilder configBuilder = Configuration.builder();
    // to inherit from the global configuration
    configBuilder.copyFrom(config);
    configBuilder.loadFromFile(mainAnalyisisConfigFile);
    Configuration subConfig = configBuilder.build();
    subShutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
    LogManager subLogger = logger.withComponentName("SingleAlgorithmRunPhase");
    CoreComponentsFactory factory =
        new CoreComponentsFactory(subConfig, subLogger, subShutdownManager
            .getNotifier());
    reached = factory.createReachedSet();
    if (runCBMCTool) {
      checkIfOneValidFile(programDenotation);
      algorithm = new ExternalCBMCAlgorithm(programDenotation, subConfig, subLogger);
    } else {
      final SpecAutomatonCompositionType specComposition = initialStatesFor.contains
          (InitialStatesFor.TARGET) ? SpecAutomatonCompositionType.BACKWARD_TO_ENTRY_SPEC :
                                                           SpecAutomatonCompositionType.TARGET_SPEC;
      subCpa = factory.createCPA(cfa, stats, specComposition);
      // set-up CPA info on global since we are about to run this CPA
      GlobalInfo.getInstance().setUpInfoFromCPA(subCpa);
      algorithm = factory.createAlgorithm(subCpa, programDenotation, cfa, stats);
      // initialize reached set
      if (algorithm instanceof ImpactAlgorithm) {
        ImpactAlgorithm impactAlgo = (ImpactAlgorithm) algorithm;
        reached.add(impactAlgo.getInitialState(cfa.getMainFunction()), impactAlgo
            .getInitialPrecision(cfa.getMainFunction()));
      } else {
        initializeReachedSet(reached, subCpa, cfa.getMainFunction(), cfa, factory,
            subShutdownManager
                .getNotifier(), subLogger, subConfig);
      }
    }

    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus postAction() throws Exception {
    // the same as {@link #prevAction}
    if (subCpa != null) {
      CPAs.closeCpaIfPossible(subCpa, logger);
    }
    CPAs.closeIfPossible(algorithm, logger);
    return CPAPhaseStatus.SUCCESS;
  }

  @Override
  protected CPAPhaseStatus runPhase() throws Exception {
    AlgorithmStatus runStat = AlgorithmStatus.SOUND_AND_PRECISE;

    stats.startAnalysisTimer();
    try {
      do {
        runStat = runStat.update(algorithm.run(reached));
      } while (!stopAfterError && reached.hasWaitingState());
      currResult = runStat;
    } finally {
      subShutdownManager.requestShutdown("SingleAlgorithmRunPhase terminated");
      // store the algorithm status and reached set as the phase result
      currResult = new AlgorithmPhaseResult(runStat, reached);
    }
    stats.stopAnalysisTimer();

    return CPAPhaseStatus.SUCCESS;
  }

  private void checkIfOneValidFile(String fileDenote) throws InvalidConfigurationException {
    if (fileDenote.contains(",")) {
      throw new InvalidConfigurationException("Multiple files not supported");
    }
  }

  private void initializeReachedSet(
      final ReachedSet pReached, final ConfigurableProgramAnalysis
      pCpa, final FunctionEntryNode pEntry, final CFA pCfa, final CoreComponentsFactory pFactory,
      final ShutdownNotifier pNotifier, final LogManager pLogger, final Configuration pConfig)
      throws InvalidConfigurationException {
    for (InitialStatesFor isf : initialStatesFor) {
      final ImmutableSet<? extends CFANode> initialLocations;
      switch (isf) {
        case ENTRY:
          initialLocations = ImmutableSet.of(pEntry);
          break;
        case EXIT:
          initialLocations = ImmutableSet.of(pEntry.getExitNode());
          break;
        case FUNCTION_ENTRIES:
          initialLocations = ImmutableSet.copyOf(pCfa.getAllFunctionHeads());
          break;
        case FUNCTION_SINKS:
          // TODO: why adding loop heads?
          initialLocations = ImmutableSet.<CFANode>builder().addAll(getAllEndlessLoopHeads(pCfa
              .getLoopStructure().get()))
              .addAll(getAllFunctionExitNodes(pCfa))
              .build();
          break;
        case PROGRAM_SINKS:
          Builder builder = ImmutableSet.builder().addAll(getAllEndlessLoopHeads(pCfa
              .getLoopStructure().get()));
          if (pCfa.getAllNodes().contains(pEntry.getExitNode())) {
            builder.add(pEntry.getExitNode());
          }
          initialLocations = builder.build();
          break;
        case TARGET:
          TargetLocationProvider provider = new TargetLocationProviderImpl(pFactory
              .getReachedSetFactory(), pNotifier, pLogger, pConfig, pCfa);
          initialLocations = provider.tryGetAutomatonTargetLocations(pEntry);
          break;
        default:
          throw new AssertionError("Unhandled case statement: " + initialStatesFor);
      }
      // finally, add these locations to the reached set
      addToInitialReachedSet(initialLocations, isf, pReached, pCpa);
    }

    if (!pReached.hasWaitingState()) {
      throw new InvalidConfigurationException("No analysis target found");
    }
  }

  private Set<CFANode> getAllFunctionExitNodes(CFA cfa) {
    Set<CFANode> funcExitNodes = new HashSet<>();
    for (FunctionEntryNode node : cfa.getAllFunctionHeads()) {
      FunctionExitNode exit = node.getExitNode();
      if (cfa.getAllNodes().contains(exit)) {
        funcExitNodes.add(exit);
      }
    }
    return funcExitNodes;
  }

  private Set<CFANode> getAllEndlessLoopHeads(LoopStructure structure) {
    ImmutableCollection<Loop> loops = structure.getAllLoops();
    Set<CFANode> loopHeads = new HashSet<>();
    for (Loop loop : loops) {
      if (loop.getOutgoingEdges().isEmpty()) {
        for (CFANode head : loop.getLoopHeads()) {
          loopHeads.add(head);
        }
      }
    }

    return loopHeads;
  }

  private void addToInitialReachedSet(
      final Set<? extends CFANode> pLocations, final Object
      pPartitionKey, final ReachedSet pReached, final ConfigurableProgramAnalysis pCpa) {
    for (CFANode loc : pLocations) {
      StateSpacePartition partition = partitionInitialStates ? StateSpacePartition
          .getPartitionWithKey(pPartitionKey) : StateSpacePartition.getDefaultPartition();

      if (functionWisePartition && (pPartitionKey == InitialStatesFor.FUNCTION_ENTRIES ||
          pPartitionKey == InitialStatesFor.FUNCTION_SINKS)) {
        partition = StateSpacePartition.getPartitionWithKey(loc.getFunctionName());
      }

      AbstractState initialState = pCpa.getInitialState(loc, partition);
      Precision initialPrecision = pCpa.getInitialPrecision(loc, partition);
      pReached.add(initialState, initialPrecision);
    }
  }

}
