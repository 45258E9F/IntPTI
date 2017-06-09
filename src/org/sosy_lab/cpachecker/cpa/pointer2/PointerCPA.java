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
package org.sosy_lab.cpachecker.cpa.pointer2;

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

/**
 * Instances of this class are configurable program analyses for analyzing a
 * program to gain information about pointer aliasing.
 */
@Options(prefix = "cpa.pointer2")
public class PointerCPA implements ConfigurableProgramAnalysis {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PointerCPA.class);
  }

  @Option(secure = true, name = "merge", values = {"SEP", "JOIN"}, toUppercase = true,
      description = "which merge operator to use for pointer CPA")
  private String mergeType = "SEP";

  private final AbstractDomain abstractDomain;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final TransferRelation transferRelation;
  private final PrecisionAdjustment precisionAdjust;

  public PointerCPA(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
    abstractDomain = Pointer2Domain.INSTANCE;
    transferRelation = new Pointer2TransferRelation();
    if (mergeType.equals("JOIN")) {
      mergeOperator = new MergeJoinOperator(abstractDomain);
    } else {
      mergeOperator = MergeSepOperator.getInstance();
    }
    stopOperator = new StopSepOperator(abstractDomain);
    precisionAdjust = StaticPrecisionAdjustment.getInstance();
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
    return precisionAdjust;
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) {
    return Pointer2State.INITIAL_STATE;
  }

  @Override
  public Precision getInitialPrecision(
      CFANode node, StateSpacePartition partition) {
    return SingletonPrecision.getInstance();
  }
}
