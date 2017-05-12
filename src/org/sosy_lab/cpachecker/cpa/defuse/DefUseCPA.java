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
package org.sosy_lab.cpachecker.cpa.defuse;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Options(prefix = "cpa.defuse")
public class DefUseCPA implements ConfigurableProgramAnalysis {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(DefUseCPA.class);
  }

  @Option(secure = true, name = "merge", values = {"sep", "join"},
      description = "which merge operator to use for DefUseCPA")
  private String mergeType = "sep";

  private AbstractDomain abstractDomain;
  private TransferRelation transferRelation;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;

  private DefUseCPA(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
    this.abstractDomain = new DefUseDomain();

    this.transferRelation = new DefUseTransferRelation();

    this.mergeOperator = null;
    if (mergeType.equals("sep")) {
      this.mergeOperator = MergeSepOperator.getInstance();
    } else if (mergeType.equals("join")) {
      this.mergeOperator = new MergeJoinOperator(abstractDomain);
    }

    this.stopOperator = new StopSepOperator(abstractDomain);
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
    Set<DefUseDefinition> defUseDefinitions = new HashSet<>();
    if (pNode instanceof CFunctionEntryNode) {
      List<String> parameterNames = ((CFunctionEntryNode) pNode).getFunctionParameterNames();

      for (String parameterName : parameterNames) {
        DefUseDefinition newDef = new DefUseDefinition(parameterName, null);
        defUseDefinitions.add(newDef);
      }
    }

    return new DefUseState(defUseDefinitions);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return SingletonPrecision.getInstance();
  }
}
