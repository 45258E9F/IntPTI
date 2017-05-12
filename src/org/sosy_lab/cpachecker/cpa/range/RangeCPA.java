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
package org.sosy_lab.cpachecker.cpa.range;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
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
import org.sosy_lab.cpachecker.core.interfaces.FunctionRegistrant;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.summary.instance.range.RangeFunctionPrecondition;
import org.sosy_lab.cpachecker.cpa.range.util.RangeFunctionAdapter;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

import java.util.Collection;
import java.util.List;

@Options(prefix = "cpa.range")
public class RangeCPA implements ConfigurableProgramAnalysis, ProofChecker, FunctionRegistrant {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(RangeCPA.class);
  }

  @Option(secure = true, name = "merge", toUppercase = true, values = {"SEP", "JOIN"},
      description = "which type of merge operator to use for range analysis")
  private String mergeType = "SEP";

  private AbstractDomain abstractDomain;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private TransferRelation transferRelation;
  private PrecisionAdjustment precisionAdjustment;

  @SuppressWarnings("unchecked")
  private RangeCPA(final Configuration pConfig, final LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    abstractDomain = RangeDomain.INSTANCE;
    mergeOperator = mergeType.equals("SEP") ? MergeSepOperator.getInstance() : new
        MergeJoinOperator(abstractDomain);
    stopOperator = new StopSepOperator(abstractDomain);
    transferRelation = new RangeTransferRelation(pConfig, pLogger);
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
    assert (transferRelation instanceof RangeTransferRelation);
    RangeState initState;
    if(node instanceof CFunctionEntryNode) {
      CFunctionEntryNode entryNode = (CFunctionEntryNode) node;
      String funcName = entryNode.getFunctionName();
      RangeState preState = RangeFunctionPrecondition.getPrecondition(funcName);
      if(preState != null) {
        initState = RangeState.copyOf(preState);
      } else {
        try {
          initState = new RangeState();
          List<CParameterDeclaration> parameters = entryNode.getFunctionParameters();
          for(CParameterDeclaration parameter : parameters) {
            initState = ((RangeTransferRelation) transferRelation).initializeDeclaration
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

  @Override
  public boolean areAbstractSuccessors(
      AbstractState state,
      List<AbstractState> otherStates,
      CFAEdge cfaEdge,
      Collection<? extends AbstractState> successors)
      throws CPATransferException, InterruptedException {
    try {
      Collection<? extends AbstractState> computedSuccessors = transferRelation
          .getAbstractSuccessorsForEdge(state, otherStates, SingletonPrecision.getInstance(),
              cfaEdge);
      boolean found;
      for (AbstractState successor : computedSuccessors) {
        found = false;
        for (AbstractState e : successors) {
          if (isCoveredBy(successor, e)) {
            found = true;
            break;
          }
        }
        if (!found) {
          return false;
        }
      }
    } catch (CPAException e) {
      throw new CPATransferException("Cannot compare abstract successors", e);
    }
    return true;
  }

  @Override
  public boolean isCoveredBy(
      AbstractState state, AbstractState otherState) throws CPAException, InterruptedException {
    return abstractDomain.isLessOrEqual(state, otherState);
  }

  @Override
  public boolean retrieveCall(CFunctionCallExpression pCFunctionCallExpression) {
    return RangeFunctionAdapter.instance().isRegistered(pCFunctionCallExpression);
  }
}
