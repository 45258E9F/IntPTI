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
package org.sosy_lab.cpachecker.cpa.conditions.global;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.IdentityTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.NoOpReducer;
import org.sosy_lab.cpachecker.core.defaults.SingletonAbstractState;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopAlwaysOperator;
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
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;


public class GlobalConditionsCPA
    implements ConfigurableProgramAnalysisWithBAM, AdjustableConditionCPA, ProofChecker {

  private final PrecisionAdjustment precisionAdjustment;
  private final GlobalConditionsThresholds thresholds;

  private final AbstractDomain domain;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(GlobalConditionsCPA.class);
  }

  private GlobalConditionsCPA(Configuration config, LogManager logger)
      throws InvalidConfigurationException {
    thresholds = new GlobalConditionsThresholds(config, logger);

    if (thresholds.isLimitEnabled()) {
      logger.log(Level.INFO, "Analyzing with the following", thresholds);
      GlobalConditionsSimplePrecisionAdjustment prec =
          new GlobalConditionsSimplePrecisionAdjustment(logger, thresholds);

      if (thresholds.getReachedSetSizeThreshold() >= 0) {
        precisionAdjustment = new GlobalConditionsPrecisionAdjustment(logger, thresholds, prec);
      } else {
        precisionAdjustment = prec;
      }

    } else {
      logger.log(Level.WARNING,
          "GlobalConditionsCPA used without any limits, you can remove it from the list of CPAs.");
      precisionAdjustment = StaticPrecisionAdjustment.getInstance();
    }

    domain = new FlatLatticeDomain(SingletonAbstractState.INSTANCE);
  }

  @Override
  public boolean adjustPrecision() {
    return thresholds.adjustPrecision();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return SingletonAbstractState.INSTANCE;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public StopOperator getStopOperator() {
    return StopAlwaysOperator.getInstance();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return IdentityTransferRelation.INSTANCE;
  }

  @Override
  public Reducer getReducer() {
    return NoOpReducer.getInstance();
  }

  @Override
  public boolean areAbstractSuccessors(
      AbstractState pElement,
      List<AbstractState> otherStates,
      CFAEdge pCfaEdge,
      Collection<? extends AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    return pSuccessors.size() == 1 && pSuccessors.contains(pElement);
  }

  @Override
  public boolean isCoveredBy(AbstractState pElement, AbstractState pOtherElement)
      throws CPAException {
    return pElement == pOtherElement;
  }
}
