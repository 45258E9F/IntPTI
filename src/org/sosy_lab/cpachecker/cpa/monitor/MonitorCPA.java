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
package org.sosy_lab.cpachecker.cpa.monitor;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;

import java.util.Collection;

public class MonitorCPA extends AbstractSingleWrapperCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(MonitorCPA.class);
  }

  private final AbstractDomain abstractDomain;
  private final MonitorTransferRelation transferRelation;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final PrecisionAdjustment precisionAdjustment;
  private final Statistics stats;

  private MonitorCPA(ConfigurableProgramAnalysis pCpa, Configuration config)
      throws InvalidConfigurationException {
    super(pCpa);
    abstractDomain = new FlatLatticeDomain();
    transferRelation = new MonitorTransferRelation(getWrappedCpa(), config);
    precisionAdjustment = new MonitorPrecisionAdjustment(getWrappedCpa().getPrecisionAdjustment());
    mergeOperator = new MonitorMerge(getWrappedCpa());
    stopOperator = new MonitorStop(getWrappedCpa());
    stats = new MonitorStatistics(this);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return this.abstractDomain;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new MonitorState(getWrappedCpa().getInitialState(pNode, pPartition), 0L);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return this.mergeOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return this.precisionAdjustment;
  }

  @Override
  public StopOperator getStopOperator() {
    return this.stopOperator;
  }

  @Override
  public MonitorTransferRelation getTransferRelation() {
    return this.transferRelation;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    super.collectStatistics(pStatsCollection);
  }
}