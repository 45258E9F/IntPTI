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
package org.sosy_lab.cpachecker.cpa.bind;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
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
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.access.AccessPath;

import java.util.Map;
import java.util.Set;

/**
 * This CPA analyzes how a variable (or access path) is assigned to a value
 *
 * How to handle values that are assigned in function call?
 * For instance, the value of x is modified when calling f(&x)
 * (1) inside f, binding of 'x' is tracked normally
 * (2) outside f, binding of 'x' is tracked as 'f(&x)'
 * I.e., the binding information does not flow cross functions.
 *
 * This simplification is made to ease the implementation of this CPA
 * But it may complicate the analysis process, that's a trade off.
 */
@Options(prefix = "cpa.bind") // for now, no Option is injected
public class BindCPA implements ConfigurableProgramAnalysis {

  private final AbstractDomain domain;
  private final BindTransferRelation transfer;
  private final MergeOperator merge;
  private final StopOperator stop;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(BindCPA.class);
  }

  private BindCPA(
      final Configuration pConfig,
      final LogManager pLogger,
      final CFA cfa) throws InvalidConfigurationException {
    pConfig.inject(this, BindCPA.class);

    domain = DelegateAbstractDomain.<BindState>getInstance();
    transfer = new BindTransferRelation(pConfig, pLogger);

    merge = MergeSepOperator.getInstance();
    stop = new StopSepOperator(domain);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return merge;
  }

  @Override
  public StopOperator getStopOperator() {
    return stop;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    Pair<Set<AccessPath>, Map<FunctionEntryNode, Set<AccessPath>>> result = BindUtils
        .getAllVariables(pNode);
    transfer.provideLocalVariablesOfFunctions(result.getSecond());
    transfer.setMainFunctionNode(pNode);
    return new BindState(result.getFirst());
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return SingletonPrecision.getInstance();
  }
}
