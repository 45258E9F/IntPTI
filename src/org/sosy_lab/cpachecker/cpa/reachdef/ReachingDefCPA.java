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
package org.sosy_lab.cpachecker.cpa.reachdef;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
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
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.reachingdef.ReachingDefUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/*
 * Requires preprocessing with cil to get proper result because preprocessing guarantees that
 * 1) no two variables accessible in function f, have same name in function f
 * 2) all local variables are declared at begin of function body
 *
 * If function x is called from at least two distinct functions y and z, analysis must be done together
 * with CallstackCPA.
 */
@Options(prefix = "cpa.reachdef")
public class ReachingDefCPA implements ConfigurableProgramAnalysis, ProofChecker {

  private LogManager logger;

  private AbstractDomain domain;

  private ReachingDefTransferRelation transfer;

  @Option(secure = true, name = "merge", toUppercase = true, values = {"SEP", "JOIN", "IGNORECALLSTACK"},
      description = "which merge operator to use for ReachingDefCPA")
  private String mergeType = "JOIN";

  @Option(secure = true, name = "stop", toUppercase = true, values = {"SEP", "JOIN", "IGNORECALLSTACK"},
      description = "which stop operator to use for ReachingDefCPA")
  private String stopType = "SEP";

  private StopOperator stop;
  private MergeOperator merge;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ReachingDefCPA.class);
  }

  private ReachingDefCPA(LogManager logger, Configuration config, ShutdownNotifier shutdownNotifier)
      throws InvalidConfigurationException {
    config.inject(this);
    this.logger = logger;

    domain = DelegateAbstractDomain.<ReachingDefState>getInstance();
    transfer = new ReachingDefTransferRelation(logger, shutdownNotifier);

    if (stopType.equals("SEP")) {
      stop = new StopSepOperator(domain);
    } else if (mergeType.equals("JOIN")) {
      stop = new StopJoinOperator(domain);
    } else {
      stop = new StopIgnoringCallstack();
    }
    if (mergeType.equals("SEP")) {
      merge = MergeSepOperator.getInstance();
    } else if (mergeType.equals("JOIN")) {
      merge = new MergeJoinOperator(domain);
    } else {
      merge = new MergeIgnoringCallstack();
    }
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
    logger.log(Level.FINE, "Start extracting all declared variables in program.",
        "Distinguish between local and global variables.");
    Pair<Set<String>, Map<FunctionEntryNode, Set<String>>> result =
        ReachingDefUtils.getAllVariables(pNode);
    logger.log(Level.FINE, "Extracted all declared variables.", "Create initial state.");
    transfer.provideLocalVariablesOfFunctions(result.getSecond());
    transfer.setMainFunctionNode(pNode);
    return new ReachingDefState(result.getFirst());
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
