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
package org.sosy_lab.cpachecker.cpa.dominator;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
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
import org.sosy_lab.cpachecker.cpa.location.LocationCPABackwards;

public class PostDominatorCPA implements ConfigurableProgramAnalysis {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PostDominatorCPA.class);
  }

  private org.sosy_lab.cpachecker.cpa.dominator.parametric.DominatorCPA parametricDominatorCPA;

  public PostDominatorCPA(CFA pCfa, Configuration config) throws InvalidConfigurationException {
    this.parametricDominatorCPA = new org.sosy_lab.cpachecker.cpa.dominator.parametric.DominatorCPA(
        new LocationCPABackwards(pCfa, config));
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return this.parametricDominatorCPA.getAbstractDomain();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return this.parametricDominatorCPA.getTransferRelation();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return this.parametricDominatorCPA.getMergeOperator();
  }

  @Override
  public StopOperator getStopOperator() {
    return this.parametricDominatorCPA.getStopOperator();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return this.parametricDominatorCPA.getPrecisionAdjustment();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return this.parametricDominatorCPA.getInitialState(pNode, pPartition);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return this.parametricDominatorCPA.getInitialPrecision();
  }
}
