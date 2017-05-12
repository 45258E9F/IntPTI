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
package org.sosy_lab.cpachecker.cpa.functionpointer;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.NoOpReducer;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;
import java.util.List;

public class FunctionPointerCPA implements ConfigurableProgramAnalysisWithBAM, ProofChecker {

  private AbstractDomain abstractDomain;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private TransferRelation transferRelation;
  private PrecisionAdjustment precisionAdjustment;
  private final Reducer reducer;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(FunctionPointerCPA.class);
  }

  private FunctionPointerCPA(LogManager pLogger, Configuration pConfig)
      throws InvalidConfigurationException {
    this.abstractDomain = DelegateAbstractDomain.<FunctionPointerState>getInstance();

    this.mergeOperator = MergeSepOperator.getInstance();

    this.stopOperator = new StopSepOperator(abstractDomain);
    this.transferRelation = new FunctionPointerTransferRelation(pLogger, pConfig);
    this.precisionAdjustment = StaticPrecisionAdjustment.getInstance();
    this.reducer = NoOpReducer.getInstance();
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
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return FunctionPointerState.createEmptyState();
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public Reducer getReducer() {
    return reducer;
  }

  @Override
  public boolean areAbstractSuccessors(
      AbstractState pState, List<AbstractState> otherStates, CFAEdge pCfaEdge,
      Collection<? extends AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    Collection<? extends AbstractState> computedSuccessors =
        transferRelation.getAbstractSuccessorsForEdge(
            pState, otherStates, SingletonPrecision.getInstance(), pCfaEdge);
    if (pSuccessors.size() != computedSuccessors.size()) {
      return false;
    }
    boolean found;
    try {
      for (AbstractState e1 : pSuccessors) {
        found = false;
        for (AbstractState e2 : computedSuccessors) {

          if (abstractDomain.isLessOrEqual(e2, e1)) {
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
  public boolean isCoveredBy(AbstractState pState, AbstractState pOtherState)
      throws CPAException, InterruptedException {
    return abstractDomain.isLessOrEqual(pState, pOtherState);
  }
}