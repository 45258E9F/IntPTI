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
package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

/**
 * This is an abstract class for building CPAs. It uses the flat lattice domain
 * if no other domain is given, and the standard implementations for merge-(sep|join)
 * and stop-sep.
 */
public abstract class AbstractCPA implements ConfigurableProgramAnalysis {

  private final AbstractDomain abstractDomain;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final TransferRelation transferRelation;

  protected AbstractCPA(String mergeType, String stopType, TransferRelation transfer) {
    this(mergeType, stopType, new FlatLatticeDomain(), transfer);
  }

  protected AbstractCPA(
      String mergeType,
      String stopType,
      AbstractDomain domain,
      TransferRelation transfer) {
    this.abstractDomain = domain;

    if (mergeType.equalsIgnoreCase("join")) {
      mergeOperator = new MergeJoinOperator(abstractDomain);
    } else {
      assert mergeType.equalsIgnoreCase("sep");
      mergeOperator = MergeSepOperator.getInstance();
    }

    assert stopType.equalsIgnoreCase("sep");
    stopOperator = new StopSepOperator(abstractDomain);

    this.transferRelation = transfer;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
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
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }
}
