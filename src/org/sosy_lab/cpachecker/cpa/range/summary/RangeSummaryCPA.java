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
package org.sosy_lab.cpachecker.cpa.range.summary;

import com.google.common.collect.Multimap;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.core.algorithm.summary.computer.RangeSummaryComputer;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeFunctionInstance;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeFunctionPrecondition;
import org.sosy_lab.cpachecker.cpa.range.RangeDomain;
import org.sosy_lab.cpachecker.cpa.range.RangeState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Options(prefix = "cpa.range.summary")
public class RangeSummaryCPA implements ConfigurableProgramAnalysis {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(RangeSummaryCPA.class);
  }

  @Option(secure = true, name = "merge", toUppercase = true, values = {"SEP", "JOIN"},
      description = "which type of merge operator to use in range analysis for summary")
  private String mergeType = "JOIN";

  private AbstractDomain abstractDomain;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private TransferRelation transferRelation;
  private PrecisionAdjustment precisionAdjustment;

  private RangeSummaryCPA(final Configuration pConfig, final LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    abstractDomain = RangeDomain.INSTANCE;
    mergeOperator = mergeType.equals("SEP") ? MergeSepOperator.getInstance() : new
        MergeJoinOperator(abstractDomain);
    stopOperator = new StopSepOperator(abstractDomain);
    transferRelation = new RangeSummaryTransferRelation(pConfig, pLogger);
    precisionAdjustment = StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) {
    assert (transferRelation instanceof RangeSummaryTransferRelation);
    // reset the summary info before analyzing from a new entry
    ((RangeSummaryTransferRelation) transferRelation).clearPerFunction();
    // initialize function entry
    RangeState initState;
    if(node instanceof CFunctionEntryNode) {
      CFunctionEntryNode entryNode = (CFunctionEntryNode) node;
      String funcName = entryNode.getFunctionName();
      RangeState preState = RangeFunctionPrecondition.getPrecondition(funcName);
      if(preState != null) {
        initState = RangeState.copyOf(preState);
      } else {
        // initialize each parameters
        initState = new RangeState();
        List<CParameterDeclaration> parameters = entryNode.getFunctionParameters();
        try {
          for (CParameterDeclaration parameter : parameters) {
            initState = ((RangeSummaryTransferRelation) transferRelation).initializeDeclaration
                (initState, parameter);
          }
        } catch (UnrecognizedCCodeException ex) {
          initState = new RangeState();
        }
      }
    } else {
      initState = new RangeState();
    }
    return initState;
  }

  @Override
  public Precision getInitialPrecision(
      CFANode node, StateSpacePartition partition) {
    return SingletonPrecision.getInstance();
  }

  public void setSummaryComputer(@Nonnull RangeSummaryComputer pComputer) {
    assert (transferRelation instanceof RangeSummaryTransferRelation);
    ((RangeSummaryTransferRelation) transferRelation).setSummaryComputer(pComputer);
  }

  public void getLoopSummary(
      Multimap<Loop, Pair<CFAEdge, RangeState>> pRawInternalSummary,
      Multimap<Loop, Pair<CFAEdge, RangeState>> pRawExternalSummary) {
    assert (transferRelation instanceof RangeSummaryTransferRelation);
    ((RangeSummaryTransferRelation) transferRelation).getLoopSummary(pRawInternalSummary,
        pRawExternalSummary);
  }

  @Nullable
  public RangeFunctionInstance getFunctionSummary() {
    assert (transferRelation instanceof RangeSummaryTransferRelation);
    return ((RangeSummaryTransferRelation) transferRelation).getFunctionSummary();
  }

  public void loadLoopSummary(CFAInfo cfaInfo, String funcName) {
    assert (transferRelation instanceof RangeSummaryTransferRelation);
    ((RangeSummaryTransferRelation) transferRelation).loadLoopSummary(cfaInfo, funcName);
  }

}
