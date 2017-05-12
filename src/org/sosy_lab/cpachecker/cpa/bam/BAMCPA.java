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
package org.sosy_lab.cpachecker.cpa.bam;

import com.google.common.base.Preconditions;

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.blocks.BlockToDotWriter;
import org.sosy_lab.cpachecker.cfa.blocks.builder.BlockPartitioningBuilder;
import org.sosy_lab.cpachecker.cfa.blocks.builder.ExtendedBlockPartitioningBuilder;
import org.sosy_lab.cpachecker.cfa.blocks.builder.FunctionAndLoopPartitioning;
import org.sosy_lab.cpachecker.cfa.blocks.builder.PartitioningHeuristic;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.predicate.BAMPredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

import java.util.Collection;
import java.util.List;


@Options(prefix = "cpa.bam")
public class BAMCPA extends AbstractSingleWrapperCPA implements StatisticsProvider, ProofChecker {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(BAMCPA.class);
  }

  private BlockPartitioning blockPartitioning;

  private final LogManager logger;
  private final TimedReducer reducer;
  private final BAMTransferRelation transfer;
  private final BAMPrecisionAdjustment prec;
  private final BAMMergeOperator merge;
  private final BAMStopOperator stop;
  private final BAMCPAStatistics stats;
  private final PartitioningHeuristic heuristic;
  private final CFA cfa;
  private final ProofChecker wrappedProofChecker;
  private final BAMDataManager data;

  @Option(secure = true, description =
      "Type of partitioning (FunctionAndLoopPartitioning or DelayedFunctionAndLoopPartitioning)\n"
          + "or any class that implements a PartitioningHeuristic")
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker.cfa.blocks.builder")
  private Class<? extends PartitioningHeuristic> blockHeuristic = FunctionAndLoopPartitioning.class;

  @Option(secure = true, description = "export blocks")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportBlocksPath = Paths.get("block_cfa.dot");

  @Option(name = "handleRecursiveProcedures", secure = true,
      description =
          "BAM allows to analyse recursive procedures. This strongly depends on the underlying CPA. "
              + "The current support includes only ValueAnalysis and PredicateAnalysis (with tree interpolation enabled).")
  private boolean handleRecursiveProcedures = false;

  @Option(secure = true,
      description = "This flag determines which precisions should be updated during refinement. "
          + "We can choose between the minimum number of states and all states that are necessary "
          + "to re-explore the program along the error-path.")
  private boolean doPrecisionRefinementForAllStates = false;

  @Option(secure = true,
      description = "Use more fast partitioning builder, which can not handle loops")
  private boolean useExtendedPartitioningBuilder = false;

  public BAMCPA(
      ConfigurableProgramAnalysis pCpa, Configuration config, LogManager pLogger,
      ReachedSetFactory pReachedSetFactory, ShutdownNotifier pShutdownNotifier, CFA pCfa)
      throws InvalidConfigurationException, CPAException {
    super(pCpa);
    config.inject(this);

    logger = pLogger;
    cfa = pCfa;

    if (!(pCpa instanceof ConfigurableProgramAnalysisWithBAM)) {
      throw new InvalidConfigurationException(
          "BAM needs CPAs that are capable for BAM");
    }
    Reducer wrappedReducer = ((ConfigurableProgramAnalysisWithBAM) pCpa).getReducer();
    if (wrappedReducer == null) {
      throw new InvalidConfigurationException("BAM needs CPAs that are capable for BAM");
    }

    if (pCpa instanceof ProofChecker) {
      this.wrappedProofChecker = (ProofChecker) pCpa;
    } else {
      this.wrappedProofChecker = null;
    }
    reducer = new TimedReducer(wrappedReducer);
    final BAMCache cache = new BAMCache(config, reducer, logger);
    data = new BAMDataManager(cache, pReachedSetFactory, pLogger);

    if (handleRecursiveProcedures) {

      if (cfa.getVarClassification().isPresent() && !cfa.getVarClassification().get()
          .getRelevantFields().isEmpty()) {
        // TODO remove this ugly hack as soon as possible :-)
        throw new UnsupportedCCodeException(
            "BAM does not support pointer-analysis for recursive programs.",
            cfa.getMainFunction().getLeavingEdge(0));
      }

      transfer =
          new BAMTransferRelationWithFixPointForRecursion(config, logger, this, wrappedProofChecker,
              data, pShutdownNotifier);
      stop = new BAMStopOperatorForRecursion(pCpa.getStopOperator(), transfer);
    } else {
      transfer = new BAMTransferRelation(config, logger, this, wrappedProofChecker, data,
          pShutdownNotifier);
      stop = new BAMStopOperator(pCpa.getStopOperator(), transfer);
    }

    prec = new BAMPrecisionAdjustment(pCpa.getPrecisionAdjustment(), data, transfer, logger);
    merge = new BAMMergeOperator(pCpa.getMergeOperator(), transfer);

    stats = new BAMCPAStatistics(this, data, config, logger);
    heuristic = getPartitioningHeuristic();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    if (blockPartitioning == null) {
      BlockPartitioningBuilder blockBuilder;
      if (useExtendedPartitioningBuilder) {
        blockBuilder = new ExtendedBlockPartitioningBuilder();
      } else {
        blockBuilder = new BlockPartitioningBuilder();
      }
      blockPartitioning = heuristic.buildPartitioning(pNode, blockBuilder);

      if (exportBlocksPath != null) {
        BlockToDotWriter writer = new BlockToDotWriter(blockPartitioning);
        writer.dump(exportBlocksPath, logger);
      }

      transfer.setBlockPartitioning(blockPartitioning);

      BAMPredicateCPA predicateCpa =
          ((WrapperCPA) getWrappedCpa()).retrieveWrappedCpa(BAMPredicateCPA.class);
      if (predicateCpa != null) {
        predicateCpa.setPartitioning(blockPartitioning);
      }
    }
    return getWrappedCpa().getInitialState(pNode, pPartition);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return getWrappedCpa().getInitialPrecision(pNode, pPartition);
  }

  private PartitioningHeuristic getPartitioningHeuristic()
      throws CPAException, InvalidConfigurationException {
    return Classes
        .createInstance(PartitioningHeuristic.class, blockHeuristic, new Class[]{LogManager.class,
            CFA.class}, new Object[]{logger, cfa}, CPAException.class);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return getWrappedCpa().getAbstractDomain();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return merge;
  }

  @Override
  public StopOperator getStopOperator() {
    return stop;
  }

  @Override
  public BAMPrecisionAdjustment getPrecisionAdjustment() {
    return prec;
  }

  @Override
  public BAMTransferRelation getTransferRelation() {
    return transfer;
  }

  TimedReducer getReducer() {
    return reducer;
  }

  @Override
  protected ConfigurableProgramAnalysis getWrappedCpa() {
    // override for visibility
    return super.getWrappedCpa();
  }

  BlockPartitioning getBlockPartitioning() {
    Preconditions.checkNotNull(blockPartitioning);
    return blockPartitioning;
  }

  BAMDataManager getData() {
    Preconditions.checkNotNull(data);
    return data;
  }

  LogManager getLogger() {
    return logger;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    super.collectStatistics(pStatsCollection);
  }

  BAMCPAStatistics getStatistics() {
    return stats;
  }

  @Override
  public boolean areAbstractSuccessors(
      AbstractState pState, List<AbstractState> otherStates, CFAEdge pCfaEdge,
      Collection<? extends AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    Preconditions
        .checkNotNull(wrappedProofChecker, "Wrapped CPA has to implement ProofChecker interface");
    return transfer.areAbstractSuccessors(pState, otherStates, pCfaEdge, pSuccessors);
  }

  @Override
  public boolean isCoveredBy(AbstractState pState, AbstractState pOtherState)
      throws CPAException, InterruptedException {
    Preconditions
        .checkNotNull(wrappedProofChecker, "Wrapped CPA has to implement ProofChecker interface");
    return wrappedProofChecker.isCoveredBy(pState, pOtherState);
  }

  boolean doPrecisionRefinementForAllStates() {
    return doPrecisionRefinementForAllStates;
  }
}
