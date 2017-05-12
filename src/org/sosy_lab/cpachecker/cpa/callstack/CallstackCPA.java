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
package org.sosy_lab.cpachecker.cpa.callstack;

import com.google.common.collect.Iterables;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop.CFASingleLoopTransformation;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class CallstackCPA extends AbstractCPA
    implements ConfigurableProgramAnalysisWithBAM, ProofChecker {

  private final Reducer reducer;

  private final CFA cfa;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(CallstackCPA.class);
  }

  public CallstackCPA(Configuration config, LogManager pLogger, CFA pCFA)
      throws InvalidConfigurationException {
    super("sep", "sep",
        new DomainInitializer(config).initializeDomain(),
        new TransferInitializer(config).initializeTransfer(config, pLogger));
    this.cfa = pCFA;
    this.reducer = new CallstackReducer();
  }

  @Override
  public Reducer getReducer() {
    return reducer;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    if (cfa.getLoopStructure().isPresent()) {
      LoopStructure loopStructure = cfa.getLoopStructure().get();
      Collection<Loop> artificialLoops = loopStructure.getLoopsForFunction(
          CFASingleLoopTransformation.ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME);

      if (!artificialLoops.isEmpty()) {
        Loop singleLoop = Iterables.getOnlyElement(artificialLoops);
        if (singleLoop.getLoopNodes().contains(pNode)) {
          return new CallstackState(
              null,
              CFASingleLoopTransformation.ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME,
              pNode
          );
        }
      }
    }
    return new CallstackState(null, pNode.getFunctionName(), pNode);
  }

  @Override
  public boolean areAbstractSuccessors(
      AbstractState pElement, List<AbstractState> otherStates, CFAEdge pCfaEdge,
      Collection<? extends AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    Collection<? extends AbstractState> computedSuccessors =
        getTransferRelation().getAbstractSuccessorsForEdge(
            pElement, otherStates, SingletonPrecision.getInstance(), pCfaEdge);
    if (!(pSuccessors instanceof Set) || !(computedSuccessors instanceof Set)
        || pSuccessors.size() != computedSuccessors.size()) {
      return false;
    }
    boolean found;
    for (AbstractState e1 : pSuccessors) {
      found = false;
      for (AbstractState e2 : computedSuccessors) {
        if (((CallstackState) e1).sameStateInProofChecking((CallstackState) e2)) {
          found = true;
          break;
        }
      }
      if (!found) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isCoveredBy(AbstractState pElement, AbstractState pOtherElement)
      throws CPAException, InterruptedException {
    return (getAbstractDomain().isLessOrEqual(pElement, pOtherElement))
        || ((CallstackState) pElement)
        .sameStateInProofChecking((CallstackState) pOtherElement);
  }

  @Options(prefix = "cpa.callstack")
  private static class DomainInitializer {

    @Option(secure = true, name = "domain", toUppercase = true, values = {"FLAT", "FLATPCC"},
        description = "which abstract domain to use for callstack cpa, typically FLAT which is faster since it uses only object equivalence")
    private String domainType = "FLAT";

    public DomainInitializer(Configuration pConfig) throws InvalidConfigurationException {
      pConfig.inject(this);
    }

    public AbstractDomain initializeDomain() throws InvalidConfigurationException {
      switch (domainType) {
        case "FLAT":
          return new FlatLatticeDomain();
        case "FLATPCC":
          return new CallstackPCCAbstractDomain();
        default:
          throw new InvalidConfigurationException("Unknown domain type for callstack cpa.");
      }
    }
  }

  @Options(prefix = "cpa.callstack")
  private static class TransferInitializer {

    @Option(description = "analyse the CFA backwards", secure = true)
    private boolean traverseBackwards = false;

    public TransferInitializer(Configuration pConfig) throws InvalidConfigurationException {
      pConfig.inject(this);
    }

    public TransferRelation initializeTransfer(Configuration pConfig, LogManager pLogger)
        throws InvalidConfigurationException {
      if (traverseBackwards) {
        return new CallstackTransferRelationBackwards(pConfig, pLogger);
      } else {
        return new CallstackTransferRelation(pConfig, pLogger);
      }
    }
  }

}