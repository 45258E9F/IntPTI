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
package org.sosy_lab.cpachecker.cpa.PropertyChecker;

import com.google.common.base.Preconditions;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PropertyChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.pcc.propertychecker.PropertyCheckerBuilder;

import java.util.Collection;
import java.util.List;

@Options
public class PropertyCheckerCPA extends AbstractSingleWrapperCPA implements ProofChecker {

  @Option(secure = true,
      name = "cpa.propertychecker.className",
      description = "Qualified name for class which checks that the computed abstraction adheres to the desired property.")
  private String checkerClass =
      "org.sosy_lab.cpachecker.pcc.propertychecker.DefaultPropertyChecker";
  @Option(secure = true,
      name = "cpa.propertychecker.parameters",
      description =
          "List of parameters for constructor of propertychecker.className. Parameter values are " +
              "specified in the order the parameters are defined in the respective constructor. Every parameter value is finished "
              +
              "with \",\". The empty string represents an empty parameter list.")
  private String checkerParamList = "";

  private final PropertyChecker propChecker;
  private final ProofChecker wrappedProofChecker;

  public PropertyCheckerCPA(ConfigurableProgramAnalysis pCpa, Configuration pConfig)
      throws InvalidConfigurationException {
    super(pCpa);
    pConfig.inject(this);
    propChecker = PropertyCheckerBuilder.buildPropertyChecker(checkerClass, checkerParamList);
    if (pCpa instanceof ProofChecker) {
      wrappedProofChecker = (ProofChecker) pCpa;
    } else {
      wrappedProofChecker = null;
    }
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PropertyCheckerCPA.class);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return getWrappedCpa().getAbstractDomain();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return getWrappedCpa().getTransferRelation();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return getWrappedCpa().getMergeOperator();
  }

  @Override
  public StopOperator getStopOperator() {
    return getWrappedCpa().getStopOperator();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return getWrappedCpa().getPrecisionAdjustment();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return getWrappedCpa().getInitialState(pNode, pPartition);
  }

  public PropertyChecker getPropChecker() {
    return propChecker;
  }

  @Override
  public boolean areAbstractSuccessors(
      AbstractState pState, List<AbstractState> otherStates, CFAEdge pCfaEdge,
      Collection<? extends AbstractState> pSuccessors) throws CPATransferException,
                                                              InterruptedException {
    Preconditions
        .checkNotNull(wrappedProofChecker, "Wrapped CPA must implement the ProofChecker interface");
    return wrappedProofChecker.areAbstractSuccessors(pState, otherStates, pCfaEdge, pSuccessors);
  }

  @Override
  public boolean isCoveredBy(AbstractState pState, AbstractState pOtherState) throws CPAException,
                                                                                     InterruptedException {
    Preconditions
        .checkNotNull(wrappedProofChecker, "Wrapped CPA must implement the ProofChecker interface");
    return wrappedProofChecker.isCoveredBy(pState, pOtherState);
  }

}
