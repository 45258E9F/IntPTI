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
package org.sosy_lab.cpachecker.cpa.andersen;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StopNeverOperator;
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

@Options(prefix = "cpa.pointerA")
public class AndersenCPA implements ConfigurableProgramAnalysisWithBAM {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(AndersenCPA.class);
  }

  @Option(secure = true, name = "merge", toUppercase = true, values = {"SEP", "JOIN"},
      description = "which merge operator to use for PointerACPA")
  private String mergeType = "JOIN";

  @Option(secure = true, name = "stop", toUppercase = true, values = {"SEP", "JOIN", "NEVER"},
      description = "which stop operator to use for PointerACPA")
  private String stopType = "SEP";

  private AbstractDomain abstractDomain;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private TransferRelation transferRelation;
  private PrecisionAdjustment precisionAdjustment;

  private final Configuration config;
  private final LogManager logger;

  private AndersenCPA(Configuration config, LogManager logger)
      throws InvalidConfigurationException {
    this.config = config;
    this.logger = logger;

    config.inject(this);

    abstractDomain = DelegateAbstractDomain.<AndersenState>getInstance();
    transferRelation = new AndersenTransferRelation(logger);
    mergeOperator = initializeMergeOperator();
    stopOperator = initializeStopOperator();
    precisionAdjustment = StaticPrecisionAdjustment.getInstance();
  }

  private MergeOperator initializeMergeOperator() {
    if (mergeType.equals("SEP")) {
      return MergeSepOperator.getInstance();

    } else if (mergeType.equals("JOIN")) {
      return new MergeJoinOperator(abstractDomain);
    }

    return null;
  }

  private StopOperator initializeStopOperator() {
    if (stopType.equals("SEP")) {
      return new StopSepOperator(abstractDomain);

    } else if (stopType.equals("JOIN")) {
      return new StopJoinOperator(abstractDomain);

    } else if (stopType.equals("NEVER")) {
      return new StopNeverOperator();
    }

    return null;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
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
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new AndersenState();
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return new AndersenPrecision();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  protected Configuration getConfiguration() {
    return config;
  }

  protected LogManager getLogger() {
    return logger;
  }

  @Override
  public Reducer getReducer() {
    return null;
  }
}
