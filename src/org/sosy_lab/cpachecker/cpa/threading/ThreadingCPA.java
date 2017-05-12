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
package org.sosy_lab.cpachecker.cpa.threading;

import com.google.common.base.Preconditions;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
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
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;

import java.util.Collection;

public class ThreadingCPA implements ConfigurableProgramAnalysis, StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ThreadingCPA.class);
  }

  private final AbstractDomain abstractDomain = new FlatLatticeDomain();
  private final MergeOperator mergeOperator = MergeSepOperator.getInstance();
  private final StopOperator stopOperator = new StopSepOperator(abstractDomain);
  private final ConfigurableProgramAnalysis locationCPA;
  private final ConfigurableProgramAnalysis callstackCPA;
  private final ThreadingTransferRelation transferRelation;

  public ThreadingCPA(Configuration config, LogManager pLogger, CFA pCfa)
      throws InvalidConfigurationException {
    locationCPA = new LocationCPA(pCfa, config);
    callstackCPA = new CallstackCPA(config, pLogger, pCfa);
    transferRelation =
        new ThreadingTransferRelation(config, callstackCPA, locationCPA, pCfa, pLogger);
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
    Preconditions.checkNotNull(pNode);
    String mainThread = pNode.getFunctionName();
    ThreadingState ts = new ThreadingState();
    return ts.addThreadAndCopy(mainThread, ThreadingState.MIN_THREAD_NUM,
        callstackCPA.getInitialState(pNode, pPartition),
        locationCPA.getInitialState(pNode, pPartition));
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition partition) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
  }
}
