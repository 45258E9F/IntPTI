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
package org.sosy_lab.cpachecker.cpa.dominator.parametric;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class DominatorCPA {

  private ConfigurableProgramAnalysis cpa;

  private DominatorDomain abstractDomain;
  private TransferRelation transferRelation;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private PrecisionAdjustment precisionAdjustment;

  public DominatorCPA(ConfigurableProgramAnalysis cpa) {
    this.cpa = cpa;

    this.abstractDomain = new DominatorDomain(this.cpa);
    this.transferRelation = new DominatorTransferRelation(this.cpa);
    this.mergeOperator = new MergeJoinOperator(abstractDomain);
    this.stopOperator = new StopSepOperator(abstractDomain);
    this.precisionAdjustment = StaticPrecisionAdjustment.getInstance();
  }

  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  public StopOperator getStopOperator() {
    return stopOperator;
  }

  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    AbstractState dominatedInitialState_tmp = this.cpa.getInitialState(pNode, pPartition);

    AbstractState dominatedInitialState = dominatedInitialState_tmp;

    DominatorState initialState = new DominatorState(dominatedInitialState);

    initialState.update(dominatedInitialState);

    return initialState;
  }

  public Precision getInitialPrecision() {
    return null;
  }
}
