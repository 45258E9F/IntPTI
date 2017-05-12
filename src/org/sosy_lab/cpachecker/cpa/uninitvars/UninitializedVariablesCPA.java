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
package org.sosy_lab.cpachecker.cpa.uninitvars;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
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

import java.util.Collection;

@Options(prefix = "cpa.uninitvars")
public class UninitializedVariablesCPA implements ConfigurableProgramAnalysis, StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(UninitializedVariablesCPA.class);
  }

  @Option(secure = true, description = "print warnings during analysis when uninitialized variables are used")
  private String printWarnings = "true";
  @Option(secure = true, name = "merge", values = {"sep", "join"},
      description = "which merge operator to use for UninitializedVariablesCPA?")
  private String mergeType = "sep";
  @Option(secure = true, name = "stop", values = {"sep", "join"},
      description = "which stop operator to use for UninitializedVariablesCPA?")
  private String stopType = "sep";

  private final AbstractDomain abstractDomain;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final TransferRelation transferRelation;
  private final PrecisionAdjustment precisionAdjustment;
  private final UninitializedVariablesStatistics statistics;

  private UninitializedVariablesCPA(Configuration config) throws InvalidConfigurationException {

    config.inject(this);

    UninitializedVariablesDomain domain = new UninitializedVariablesDomain();

    MergeOperator mergeOp = null;
    if (mergeType.equals("sep")) {
      mergeOp = MergeSepOperator.getInstance();
    } else if (mergeType.equals("join")) {
      mergeOp = new MergeJoinOperator(domain);
    }

    StopOperator stopOp = null;

    if (stopType.equals("sep")) {
      stopOp = new StopSepOperator(domain);
    } else if (stopType.equals("join")) {
      stopOp = new StopJoinOperator(domain);
    }

    this.abstractDomain = domain;
    this.mergeOperator = mergeOp;
    this.stopOperator = stopOp;
    this.transferRelation = new UninitializedVariablesTransferRelation(printWarnings);
    this.precisionAdjustment = StaticPrecisionAdjustment.getInstance();

    statistics = new UninitializedVariablesStatistics(printWarnings);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new UninitializedVariablesState(pNode.getFunctionName());
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }

}
