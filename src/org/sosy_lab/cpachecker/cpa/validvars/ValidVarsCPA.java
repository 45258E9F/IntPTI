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
package org.sosy_lab.cpachecker.cpa.validvars;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
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
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;
import java.util.List;

// requires that function names are unique, no two functions may have the same name although they have different signature
// currently ensured by parser
@Options(prefix = "cpa.validVars")
public class ValidVarsCPA implements ConfigurableProgramAnalysis, ProofChecker {

  @Option(secure = true, name = "merge", toUppercase = true, values = {"SEP", "JOIN"},
      description = "which merge operator to use for ValidVarsCPA")
  private String mergeType = "JOIN";

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ValidVarsCPA.class);
  }

  private final AbstractDomain domain;
  private final TransferRelation transfer;
  private final MergeOperator merge;
  private final StopOperator stop;

  public ValidVarsCPA(Configuration config) throws InvalidConfigurationException {
    config.inject(this);

    domain = new ValidVarsDomain();
    transfer = new ValidVarsTransferRelation();

    if (mergeType.equals("SEP")) {
      merge = MergeSepOperator.getInstance();
    } else {
      merge = new MergeJoinOperator(domain);
    }

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
    return new ValidVarsState(ValidVars.initial);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public boolean areAbstractSuccessors(
      AbstractState pState, List<AbstractState> otherStates, CFAEdge pCfaEdge,
      Collection<? extends AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    try {
      Collection<? extends AbstractState> computedSuccessors =
          transfer.getAbstractSuccessorsForEdge(
              pState, otherStates, SingletonPrecision.getInstance(), pCfaEdge);
      boolean found;
      for (AbstractState comp : computedSuccessors) {
        found = false;
        for (AbstractState e : pSuccessors) {
          if (isCoveredBy(comp, e)) {
            found = true;
            break;
          }
        }
        if (!found) {
          return false;
        }
      }
    } catch (CPAException e) {
      throw new CPATransferException("Cannot compare abstract successors", e);
    }
    return true;
  }

  @Override
  public boolean isCoveredBy(AbstractState pState, AbstractState pOtherState)
      throws CPAException, InterruptedException {
    return domain.isLessOrEqual(pState, pOtherState);
  }

}
