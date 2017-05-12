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
package org.sosy_lab.cpachecker.cpa.argReplay;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
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
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import java.util.Collections;

/**
 * This CPA explores the state space of a powerset domain, backed by an old reached-set.
 * Each abstract state contains the corresponding states of the old reached-set.
 * The program location of new abstract state (in the current analysis)
 * is equal to the abstract state from the reached-set.
 */
public class ARGReplayCPA implements ConfigurableProgramAnalysis {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ARGReplayCPA.class);
  }

  private final ARGReplayTransferRelation transferRelation;
  private final AbstractDomain abstractDomain;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;

  private ReachedSet reached = null;
  private ConfigurableProgramAnalysis cpa = null;

  public ARGReplayCPA() {
    transferRelation = new ARGReplayTransferRelation();
    abstractDomain = DelegateAbstractDomain.<ARGReplayState>getInstance();
    mergeOperator = new MergeJoinOperator(abstractDomain);
    stopOperator = new StopJoinOperator(abstractDomain);
  }

  /**
   * This method should be run directly after the constructor.
   */
  public void setARGAndCPA(ReachedSet pReached, ConfigurableProgramAnalysis pCpa) {
    Preconditions.checkNotNull(pReached);
    Preconditions.checkState(this.reached == null, "ReachedSet should only be set once.");
    this.reached = pReached;

    Preconditions.checkNotNull(pCpa);
    Preconditions.checkState(this.cpa == null, "CPA should only be set once.");
    this.cpa = pCpa;
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
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    Preconditions.checkNotNull(reached);
    return new ARGReplayState(Collections.singleton((ARGState) reached.getFirstState()), cpa);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return SingletonPrecision.getInstance();
  }
}
